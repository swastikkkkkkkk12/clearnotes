package com.clearnotes.controller;

import com.clearnotes.dto.recording.NoteItemResponse;
import com.clearnotes.dto.recording.SetNoteStateRequest;
import com.clearnotes.dto.recording.TranscriptSegmentResponse;
import com.clearnotes.dto.recording.UpdateNoteRequest;
import com.clearnotes.service.RecordingNotesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recordings")
public class RecordingNotesController {
    private final RecordingNotesService notes;

    public RecordingNotesController(RecordingNotesService notes) { this.notes = notes; }

    @GetMapping("/{recordingId}/notes")
    public List<NoteItemResponse> getNotes(@AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId,
                                           @RequestParam(defaultValue = "false") boolean includeDismissed) {
        return notes.getNotes(recordingId, Long.valueOf(jwt.getSubject()), includeDismissed);
    }

    @GetMapping("/{recordingId}/transcript")
    public List<TranscriptSegmentResponse> getTranscript(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long recordingId) {
        return notes.getTranscript(recordingId, Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/{recordingId}/notes/{noteId}")
    public NoteItemResponse updateNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId,
                                       @PathVariable Long noteId, @Valid @RequestBody UpdateNoteRequest request) {
        return notes.updateNote(recordingId, noteId, Long.valueOf(jwt.getSubject()), request);
    }

    @DeleteMapping("/{recordingId}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId,
                           @PathVariable Long noteId) {
        notes.deleteNote(recordingId, noteId, Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/{recordingId}/notes/{noteId}/state")
    public NoteItemResponse setNoteState(@AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId,
                                         @PathVariable Long noteId,
                                         @Valid @RequestBody SetNoteStateRequest request) {
        return notes.setNoteState(recordingId, noteId, Long.valueOf(jwt.getSubject()), request);
    }
}
