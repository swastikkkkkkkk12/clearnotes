package com.clearnotes.service;

import com.clearnotes.dto.recording.NoteItemResponse;
import com.clearnotes.dto.recording.TranscriptSegmentResponse;
import com.clearnotes.dto.recording.UpdateNoteRequest;
import com.clearnotes.entity.NoteItem;
import com.clearnotes.entity.NoteType;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.NoteItemRepository;
import com.clearnotes.repository.TranscriptSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordingNotesService {
    private final NoteItemRepository notes;
    private final TranscriptSegmentRepository segments;
    private final GoogleCalendarLinkService calendarLinks;

    public RecordingNotesService(NoteItemRepository notes, TranscriptSegmentRepository segments,
                                 GoogleCalendarLinkService calendarLinks) {
        this.notes = notes;
        this.segments = segments;
        this.calendarLinks = calendarLinks;
    }

    @Transactional(readOnly = true)
    public List<NoteItemResponse> getNotes(Long recordingId, Long userId) {
        return notes.findAllByRecording_IdAndRecording_User_IdOrderByIdAsc(recordingId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TranscriptSegmentResponse> getTranscript(Long recordingId, Long userId) {
        return segments.findAllByRecording_IdAndRecording_User_IdOrderBySegmentIndexAsc(recordingId, userId)
                .stream()
                .map(segment -> new TranscriptSegmentResponse(segment.getId(), segment.getSegmentIndex(),
                        segment.getStartSeconds(), segment.getEndSeconds(), segment.getText()))
                .toList();
    }

    @Transactional
    public NoteItemResponse updateNote(Long recordingId, Long noteId, Long userId, UpdateNoteRequest request) {
        NoteItem note = findOwnedNote(recordingId, noteId, userId);
        note.setContent(request.content().trim());
        note.setOwner(clean(request.owner()));
        note.setDueDate(request.dueDate());
        if (note.getType() == NoteType.MEETING) {
            note.setMeetingDate(request.meetingDate());
            note.setMeetingStartTime(request.meetingStartTime());
            note.setMeetingEndTime(request.meetingEndTime());
        } else {
            note.setMeetingDate(null);
            note.setMeetingStartTime(null);
            note.setMeetingEndTime(null);
        }
        return toResponse(note);
    }

    @Transactional
    public void deleteNote(Long recordingId, Long noteId, Long userId) {
        notes.delete(findOwnedNote(recordingId, noteId, userId));
    }

    private NoteItem findOwnedNote(Long recordingId, Long noteId, Long userId) {
        return notes.findByIdAndRecording_IdAndRecording_User_Id(noteId, recordingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found."));
    }

    private NoteItemResponse toResponse(NoteItem note) {
        Long sourceSegmentId = note.getSourceSegment().getId();
        var link = note.getType() == NoteType.MEETING
                ? calendarLinks.create(note.getContent(), note.getMeetingDate(), note.getMeetingStartTime(),
                        note.getMeetingEndTime(), sourceSegmentId)
                : new GoogleCalendarLinkService.CalendarLink(null, null);
        return new NoteItemResponse(note.getId(), note.getType(), note.getContent(), note.getOwner(),
                note.getDueDate(), sourceSegmentId, note.getMeetingDate(), note.getMeetingStartTime(),
                note.getMeetingEndTime(), link.url(), link.message());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
