package com.clearnotes.service;

import com.clearnotes.entity.NoteItem;
import com.clearnotes.entity.NoteType;
import com.clearnotes.entity.DecisionRecord;
import com.clearnotes.entity.Recording;
import com.clearnotes.entity.TranscriptSegment;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.NoteItemRepository;
import com.clearnotes.repository.DecisionRecordRepository;
import com.clearnotes.repository.RecordingRepository;
import com.clearnotes.repository.TranscriptSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotePersistenceService {
    private final NoteItemRepository notes;
    private final RecordingRepository recordings;
    private final TranscriptSegmentRepository segments;
    private final DecisionRecordRepository decisions;

    public NotePersistenceService(NoteItemRepository notes, RecordingRepository recordings,
                                  TranscriptSegmentRepository segments, DecisionRecordRepository decisions) {
        this.notes = notes;
        this.recordings = recordings;
        this.segments = segments;
        this.decisions = decisions;
    }

    @Transactional
    public void replaceNotes(Long recordingId, List<ValidatedNote> validated) {
        Recording recording = recordings.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found."));
        Map<Long, TranscriptSegment> sourceById = segments.findAllByRecording_IdOrderBySegmentIndexAsc(recordingId)
                .stream().collect(Collectors.toMap(TranscriptSegment::getId, segment -> segment));
        notes.deleteAllByRecording_Id(recordingId);
        decisions.deleteAllByRecording_Id(recordingId);
        List<NoteItem> items = validated.stream().map(note -> {
            TranscriptSegment source = sourceById.get(note.sourceSegmentId());
            if (source == null) throw new IllegalArgumentException("Note source segment does not belong to this recording.");
            return new NoteItem(recording, source, note.type(), note.content(), note.owner(), note.dueDate(),
                    note.meetingDate(), note.meetingStartTime(), note.meetingEndTime());
        }).toList();
        notes.saveAll(items);
        decisions.saveAll(items.stream()
                .filter(note -> note.getType() == NoteType.DECISION)
                .map(note -> new DecisionRecord(recording, note.getSourceSegment(), note.getContent()))
                .toList());
    }

    public record ValidatedNote(com.clearnotes.entity.NoteType type, String content,
                                Long sourceSegmentId, String owner, LocalDate dueDate,
                                LocalDate meetingDate, LocalTime meetingStartTime, LocalTime meetingEndTime) { }
}
