package com.clearnotes.service;

import com.clearnotes.dto.recording.NoteItemResponse;
import com.clearnotes.repository.NoteItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordingNotesService {
    private final NoteItemRepository notes;
    private final GoogleCalendarLinkService calendarLinks;

    public RecordingNotesService(NoteItemRepository notes, GoogleCalendarLinkService calendarLinks) {
        this.notes = notes;
        this.calendarLinks = calendarLinks;
    }

    @Transactional(readOnly = true)
    public List<NoteItemResponse> getNotes(Long recordingId, Long userId) {
        return notes.findAllByRecording_IdAndRecording_User_IdOrderByIdAsc(recordingId, userId).stream()
                .map(note -> {
                    Long sourceSegmentId = note.getSourceSegment().getId();
                    var link = note.getType() == com.clearnotes.entity.NoteType.MEETING
                            ? calendarLinks.create(note.getContent(), note.getMeetingDate(), note.getMeetingStartTime(),
                                    note.getMeetingEndTime(), sourceSegmentId)
                            : new GoogleCalendarLinkService.CalendarLink(null, null);
                    return new NoteItemResponse(note.getId(), note.getType(), note.getContent(), note.getOwner(),
                            note.getDueDate(), sourceSegmentId, note.getMeetingDate(), note.getMeetingStartTime(),
                            note.getMeetingEndTime(), link.url(), link.message());
                })
                .toList();
    }
}
