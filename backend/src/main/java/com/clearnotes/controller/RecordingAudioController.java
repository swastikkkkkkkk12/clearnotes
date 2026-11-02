package com.clearnotes.controller;

import com.clearnotes.service.RecordingAudioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/recordings")
public class RecordingAudioController {
    private final RecordingAudioService audio;

    public RecordingAudioController(RecordingAudioService audio) { this.audio = audio; }

    @GetMapping("/{recordingId}/audio")
    public ResponseEntity<org.springframework.core.io.Resource> streamAudio(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long recordingId) {
        var file = audio.getAudio(recordingId, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok()
                .contentType(file.contentType())
                .header("Accept-Ranges", "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.originalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }
}
