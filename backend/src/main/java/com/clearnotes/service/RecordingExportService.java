package com.clearnotes.service;

import com.clearnotes.entity.NoteItem;
import com.clearnotes.entity.NoteType;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.exception.UnsupportedExportFormatException;
import com.clearnotes.repository.NoteItemRepository;
import com.clearnotes.repository.RecordingRepository;
import com.clearnotes.repository.TranscriptSegmentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecordingExportService {
    private final RecordingRepository recordings;
    private final TranscriptSegmentRepository segments;
    private final NoteItemRepository notes;

    public RecordingExportService(RecordingRepository recordings, TranscriptSegmentRepository segments,
                                  NoteItemRepository notes) {
        this.recordings = recordings;
        this.segments = segments;
        this.notes = notes;
    }

    @Transactional(readOnly = true)
    public ExportArtifact create(Long recordingId, Long userId, String requestedFormat) {
        var recording = recordings.findByIdAndUser_Id(recordingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found."));
        String format = requestedFormat == null ? "markdown" : requestedFormat.toLowerCase();
        if (!format.equals("markdown") && !format.equals("md") && !format.equals("pdf"))
            throw new UnsupportedExportFormatException("Choose markdown or pdf.");

        var transcript = segments.findAllByRecording_IdAndRecording_User_IdOrderBySegmentIndexAsc(recordingId, userId);
        var recordingNotes = notes.findVisibleForUser(recordingId, userId, true);
        Map<Long, BigDecimal> segmentStarts = new HashMap<>();
        transcript.forEach(segment -> segmentStarts.put(segment.getId(), segment.getStartSeconds()));
        String markdown = toMarkdown(recording.getOriginalFilename(), recording.getCreatedAt(), transcript,
                recordingNotes, segmentStarts);
        if (format.equals("pdf"))
            return new ExportArtifact(toPdf(markdown), "application/pdf", safeBase(recording.getOriginalFilename()) + ".pdf");
        return new ExportArtifact(markdown.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "text/markdown; charset=UTF-8", safeBase(recording.getOriginalFilename()) + ".md");
    }

    private static String toMarkdown(String filename, LocalDateTime createdAt, List<com.clearnotes.entity.TranscriptSegment> transcript,
                                     List<NoteItem> notes, Map<Long, BigDecimal> segmentStarts) {
        StringBuilder out = new StringBuilder("# ").append(filename).append("\n\n")
                .append("Recorded/uploaded: ").append(createdAt).append("\n\n## Decisions\n");
        appendNotes(out, notes, NoteType.DECISION, segmentStarts);
        out.append("\n## Tasks\n");
        appendNotes(out, notes, NoteType.TASK, segmentStarts);
        out.append("\n## Meetings\n");
        appendNotes(out, notes, NoteType.MEETING, segmentStarts);
        out.append("\n## Transcript\n");
        if (transcript.isEmpty()) out.append("\nNo transcript segments are available.\n");
        for (var segment : transcript)
            out.append("\n[").append(timestamp(segment.getStartSeconds())).append("–")
                    .append(timestamp(segment.getEndSeconds())).append("] ").append(segment.getText()).append('\n');
        return out.toString();
    }

    private static void appendNotes(StringBuilder out, List<NoteItem> notes, NoteType type,
                                    Map<Long, BigDecimal> segmentStarts) {
        List<NoteItem> matching = notes.stream().filter(note -> note.getType() == type).toList();
        if (matching.isEmpty()) { out.append("\nNone.\n"); return; }
        for (NoteItem note : matching) {
            BigDecimal start = segmentStarts.get(note.getSourceSegment().getId());
            out.append("\n- ").append(note.getContent());
            if (type == NoteType.TASK) {
                out.append(" — Owner: ").append(note.getOwner() == null ? "Not specified" : note.getOwner())
                        .append("; due: ").append(note.getDueDate() == null ? "Not specified" : note.getDueDate());
            }
            if (type == NoteType.MEETING) {
                out.append(" — Date: ").append(note.getMeetingDate() == null ? "Not specified" : note.getMeetingDate())
                        .append("; time: ").append(time(note.getMeetingStartTime()))
                        .append("–").append(time(note.getMeetingEndTime()));
            }
            out.append("; evidence: ").append(start == null ? "Not available" : timestamp(start));
            if (note.getReviewedAt() != null) out.append("; reviewed");
            if (note.getDismissedAt() != null) out.append("; dismissed");
        }
        out.append('\n');
    }

    private static String time(LocalTime time) { return time == null ? "Not specified" : time.toString(); }

    private static String timestamp(BigDecimal seconds) {
        long total = seconds.multiply(BigDecimal.valueOf(1000)).longValue();
        long hours = total / 3_600_000;
        long minutes = (total / 60_000) % 60;
        long secs = (total / 1000) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private static String safeBase(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static byte[] toPdf(String markdown) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String> lines = new ArrayList<>();
            for (String line : markdown.split("\\R", -1)) {
                String safe = pdfSafe(line);
                while (safe.length() > 92) {
                    int breakAt = safe.lastIndexOf(' ', 92);
                    if (breakAt < 1) breakAt = 92;
                    lines.add(safe.substring(0, breakAt));
                    safe = safe.substring(breakAt).stripLeading();
                }
                lines.add(safe);
            }

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtOffset(48, 744);
            int rows = 0;
            for (String line : lines) {
                if (rows == 48) {
                    stream.endText();
                    stream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.beginText();
                    stream.setFont(font, 10);
                    stream.newLineAtOffset(48, 744);
                    rows = 0;
                }
                stream.showText(line);
                stream.newLineAtOffset(0, -14);
                rows++;
            }
            stream.endText();
            stream.close();
            document.save(bytes);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the PDF export.", exception);
        }
    }

    private static String pdfSafe(String value) {
        StringBuilder safe = new StringBuilder();
        value.codePoints().forEach(codePoint -> {
            if (codePoint >= 32 && codePoint <= 126) safe.appendCodePoint(codePoint);
            else if (codePoint == '\t') safe.append(' ');
            else safe.append('?');
        });
        return safe.toString();
    }

    public record ExportArtifact(byte[] content, String contentType, String filename) { }
}
