package com.clearnotes.controller;

import com.clearnotes.dto.recording.NoteItemResponse;
import com.clearnotes.service.RecordingNotesService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recordings")
public class RecordingNotesController {
    private final RecordingNotesService notes;

    public RecordingNotesController(RecordingNotesService notes) { this.notes = notes; }

    @GetMapping("/{recordingId}/notes")
    public List<NoteItemResponse> getNotes(@AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId) {
        return notes.getNotes(recordingId, Long.valueOf(jwt.getSubject()));
    }
}
