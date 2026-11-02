package com.clearnotes.service;

import com.clearnotes.entity.NoteType;
import com.clearnotes.entity.TranscriptSegment;
import com.clearnotes.repository.TranscriptSegmentRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
public class NoteExtractionService {
    private static final Pattern ISO_DATE = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern WRITTEN_DATE = Pattern.compile(
            "(?i)\\b(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sept|Sep|October|Oct|November|Nov|December|Dec)\\s+(\\d{1,2})(?:st|nd|rd|th)?[,]?\\s+(\\d{4})\\b");
    private static final Pattern TWELVE_HOUR_TIME = Pattern.compile(
            "(?i)\\b(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(a\\.?m\\.?|p\\.?m\\.?)");
    private static final Pattern TWENTY_FOUR_HOUR_TIME = Pattern.compile(
            "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
    private static final Pattern WILL_ACTOR = Pattern.compile("\\b([A-Z][a-z]{1,30})\\s+will\\b");
    private static final Pattern MEETING_CUE = Pattern.compile("(?i)\\b(meeting|call|appointment|conference)\\b");
    private static final Pattern MEETING_TITLE = Pattern.compile(
            "(?i)\\b((?:[\\p{L}][\\p{L}'-]*\\s+){0,3}(?:meeting|call|appointment|conference))\\b");

    private final TranscriptSegmentRepository segments;
    private final OllamaNotesClient ollama;
    private final NotePersistenceService persistence;
    private final ObjectMapper mapper;

    public NoteExtractionService(TranscriptSegmentRepository segments, OllamaNotesClient ollama,
                                 NotePersistenceService persistence, ObjectMapper mapper) {
        this.segments = segments;
        this.ollama = ollama;
        this.persistence = persistence;
        this.mapper = mapper;
    }

    public void extract(Long recordingId) {
        List<TranscriptSegment> source = segments.findAllByRecording_IdOrderBySegmentIndexAsc(recordingId);
        if (source.isEmpty()) {
            persistence.replaceNotes(recordingId, List.of());
            return;
        }
        String prompt = buildPrompt(source);
        String correction = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            List<NotePersistenceService.ValidatedNote> validated;
            try {
                ExtractionResponse response = mapper.readValue(
                        ollama.extractJson(prompt, correction, IntStream.range(0, source.size()).boxed().toList()),
                        ExtractionResponse.class);
                validated = validate(response, source);
            } catch (JsonProcessingException | RuntimeException invalidOutput) {
                if (attempt == 1)
                    throw new IllegalStateException("Ollama returned invalid notes after one retry.", invalidOutput);
                correction = "Your previous output failed validation: " + invalidOutput.getMessage()
                        + ". Return the complete corrected object using the required JSON schema.";
                continue;
            }
            List<NotePersistenceService.ValidatedNote> notes = new ArrayList<>(validated);
            notes.addAll(extractMeetingReminders(source));
            persistence.replaceNotes(recordingId, notes);
            return;
        }
    }

    private String buildPrompt(List<TranscriptSegment> source) {
        List<PromptSegment> promptSegments = IntStream.range(0, source.size())
                .mapToObj(index -> new PromptSegment(index, source.get(index).getText())).toList();
        try {
            return "Extract concise decisions and actionable tasks from these transcript segments. Do not create meeting notes. "
                    + "Never turn vague or relative dates into a calendar date; use null unless the transcript states an exact date. "
                    + "Cite the supporting segment_index exactly as shown. Do not create a note unless supported by a segment. Transcript segments: "
                    + mapper.writeValueAsString(promptSegments);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not prepare transcript for note extraction.", exception);
        }
    }

    private static List<NotePersistenceService.ValidatedNote> validate(ExtractionResponse response,
                                                                        List<TranscriptSegment> source) {
        if (response == null || response.notes() == null || response.notes().size() > 100)
            throw new IllegalArgumentException("The notes list is missing or too large.");
        return response.notes().stream().filter(note -> !"MEETING".equals(note.type())).map(note -> {
            NoteType type;
            try { type = NoteType.valueOf(note.type()); }
            catch (RuntimeException exception) { throw new IllegalArgumentException("Unknown note type."); }
            if (note.content() == null || note.content().isBlank() || note.content().length() > 2000)
                throw new IllegalArgumentException("Note content is empty or too long.");
            if (note.sourceSegmentIndex() == null || note.sourceSegmentIndex() < 0
                    || note.sourceSegmentIndex() >= source.size())
                throw new IllegalArgumentException("A note references an unknown transcript segment index.");
            String owner = note.owner() == null || note.owner().isBlank() ? null : note.owner().trim();
            if (owner != null && owner.length() > 255)
                throw new IllegalArgumentException("Note owner is too long.");
            String evidence = evidenceNear(source, note.sourceSegmentIndex());
            if (owner != null && !evidence.toLowerCase(Locale.ROOT).contains(owner.toLowerCase(Locale.ROOT)))
                owner = null;
            LocalDate dueDate = supportedDate(note.dueDate(), evidence);
            if (type == NoteType.TASK) {
                if (owner == null) {
                    Set<String> spokenOwners = findWillActors(evidence);
                    if (spokenOwners.size() == 1) owner = spokenOwners.iterator().next();
                }
                if (dueDate == null) {
                    Set<LocalDate> explicitDeadlines = findDatesAfterBy(evidence);
                    if (explicitDeadlines.size() == 1) dueDate = explicitDeadlines.iterator().next();
                }
            }
            return new NotePersistenceService.ValidatedNote(type, note.content().trim(),
                    source.get(note.sourceSegmentIndex()).getId(), owner, dueDate, null, null, null);
        }).toList();
    }

    private static List<NotePersistenceService.ValidatedNote> extractMeetingReminders(List<TranscriptSegment> source) {
        List<NotePersistenceService.ValidatedNote> meetings = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int index = 0; index < source.size(); index++) {
            String segmentText = source.get(index).getText();
            if (!MEETING_CUE.matcher(segmentText).find()) continue;

            String evidence = segmentText + (index + 1 < source.size() ? " " + source.get(index + 1).getText() : "");
            Set<LocalDate> dates = findDates(evidence);
            LocalDate date = dates.size() == 1 ? dates.iterator().next() : null;
            List<LocalTime> times = findTimes(evidence);
            LocalTime start = times.isEmpty() ? null : times.get(0);
            LocalTime end = times.size() < 2 ? null : times.get(1);
            if (end != null && (start == null || !end.isAfter(start))) end = null;

            Matcher titleMatcher = MEETING_TITLE.matcher(segmentText);
            String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "Meeting reminder";
            title = title.replaceFirst("(?i)^(the|a|an)\\s+", "");
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
            String key = title + "|" + date + "|" + start + "|" + end;
            if (!seen.add(key)) continue;
            meetings.add(new NotePersistenceService.ValidatedNote(NoteType.MEETING, title,
                    source.get(index).getId(), null, null, date, start, end));
        }
        return meetings;
    }

    private static String evidenceNear(List<TranscriptSegment> source, int index) {
        int first = Math.max(0, index - 1);
        int lastExclusive = Math.min(source.size(), index + 2);
        return IntStream.range(first, lastExclusive).mapToObj(i -> source.get(i).getText())
                .reduce((left, right) -> left + " " + right).orElse("");
    }

    private static LocalDate supportedDate(String candidate, String evidence) {
        if (candidate == null || candidate.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(candidate);
            return findDates(evidence).contains(date) ? date : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Set<String> findWillActors(String evidence) {
        Set<String> actors = new LinkedHashSet<>();
        Matcher matcher = WILL_ACTOR.matcher(evidence);
        while (matcher.find()) {
            String actor = matcher.group(1);
            if (!Set.of("The", "We", "This", "It", "They", "He", "She").contains(actor))
                actors.add(actor);
        }
        return actors;
    }

    private static Set<LocalDate> findDatesAfterBy(String evidence) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        Matcher written = WRITTEN_DATE.matcher(evidence);
        while (written.find()) {
            String prefix = evidence.substring(0, written.start()).stripTrailing().toLowerCase(Locale.ROOT);
            if (!prefix.endsWith("by")) continue;
            try { dates.add(LocalDate.of(Integer.parseInt(written.group(3)), findMonth(written.group(1)),
                    Integer.parseInt(written.group(2)))); }
            catch (RuntimeException ignored) { }
        }
        Matcher iso = ISO_DATE.matcher(evidence);
        while (iso.find()) {
            String prefix = evidence.substring(0, iso.start()).stripTrailing().toLowerCase(Locale.ROOT);
            if (!prefix.endsWith("by")) continue;
            try { dates.add(LocalDate.parse(iso.group())); }
            catch (RuntimeException ignored) { }
        }
        return dates;
    }

    private static Set<LocalDate> findDates(String evidence) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        Matcher isoMatcher = ISO_DATE.matcher(evidence);
        while (isoMatcher.find()) {
            try { dates.add(LocalDate.parse(isoMatcher.group())); }
            catch (RuntimeException ignored) { }
        }
        Matcher writtenMatcher = WRITTEN_DATE.matcher(evidence);
        while (writtenMatcher.find()) {
            try {
                Month month = findMonth(writtenMatcher.group(1));
                dates.add(LocalDate.of(Integer.parseInt(writtenMatcher.group(3)), month,
                        Integer.parseInt(writtenMatcher.group(2))));
            } catch (RuntimeException ignored) { }
        }
        return dates;
    }

    private static Month findMonth(String token) {
        return java.util.Arrays.stream(Month.values())
                .filter(month -> month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        .toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT)))
                .findFirst().orElseThrow();
    }

    private static List<LocalTime> findTimes(String evidence) {
        List<IndexedTime> spoken = new ArrayList<>();
        Matcher twelveHour = TWELVE_HOUR_TIME.matcher(evidence);
        while (twelveHour.find()) {
            int hour = Integer.parseInt(twelveHour.group(1)) % 12;
            if (twelveHour.group(3).toLowerCase(Locale.ROOT).startsWith("p")) hour += 12;
            int minute = twelveHour.group(2) == null ? 0 : Integer.parseInt(twelveHour.group(2));
            spoken.add(new IndexedTime(twelveHour.start(), LocalTime.of(hour, minute)));
        }
        Matcher twentyFourHour = TWENTY_FOUR_HOUR_TIME.matcher(evidence);
        while (twentyFourHour.find()) {
            spoken.add(new IndexedTime(twentyFourHour.start(), LocalTime.of(
                    Integer.parseInt(twentyFourHour.group(1)), Integer.parseInt(twentyFourHour.group(2)))));
        }
        List<LocalTime> times = new ArrayList<>();
        spoken.stream().sorted(Comparator.comparingInt(IndexedTime::position)).map(IndexedTime::time)
                .forEach(time -> { if (!times.contains(time)) times.add(time); });
        return times;
    }

    private record IndexedTime(int position, LocalTime time) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtractionResponse(List<ExtractedNote> notes) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtractedNote(String type, String content,
                                @JsonProperty("source_segment_index") Integer sourceSegmentIndex,
                                String owner,
                                @JsonProperty("due_date") String dueDate) { }

    private record PromptSegment(@JsonProperty("segment_index") Integer segmentIndex, String text) { }
}
