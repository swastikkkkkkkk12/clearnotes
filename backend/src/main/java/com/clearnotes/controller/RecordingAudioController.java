package com.clearnotes.controller;

import com.clearnotes.service.RecordingAudioService;
import com.clearnotes.service.AudioPlaybackLinkService;
import com.clearnotes.dto.recording.AudioPlaybackLinkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/recordings")
public class RecordingAudioController {
    private final RecordingAudioService audio;
    private final AudioPlaybackLinkService playbackLinks;

    public RecordingAudioController(RecordingAudioService audio, AudioPlaybackLinkService playbackLinks) {
        this.audio = audio;
        this.playbackLinks = playbackLinks;
    }

    @PostMapping("/{recordingId}/audio-link")
    public AudioPlaybackLinkResponse createPlaybackLink(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long recordingId) {
        Long userId = Long.valueOf(jwt.getSubject());
        audio.getAudio(recordingId, userId); // Verify ownership and that the file still exists.
        var link = playbackLinks.create(recordingId, userId);
        return new AudioPlaybackLinkResponse(link.url(), link.expiresAt());
    }

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
