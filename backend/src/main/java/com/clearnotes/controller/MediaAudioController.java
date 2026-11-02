package com.clearnotes.controller;

import com.clearnotes.service.AudioPlaybackLinkService;
import com.clearnotes.service.RecordingAudioService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/media/audio")
public class MediaAudioController {
    private final RecordingAudioService audio;
    private final AudioPlaybackLinkService playbackLinks;

    public MediaAudioController(RecordingAudioService audio, AudioPlaybackLinkService playbackLinks) {
        this.audio = audio;
        this.playbackLinks = playbackLinks;
    }

    @GetMapping("/{recordingId}")
    public ResponseEntity<Resource> streamWithTemporaryLink(@PathVariable Long recordingId,
                                                             @RequestParam String token) {
        var userId = playbackLinks.userIdFor(token, recordingId);
        if (userId.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var file = audio.getAudio(recordingId, userId.getAsLong());
        return ResponseEntity.ok()
                .contentType(file.contentType())
                .header("Accept-Ranges", "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("Referrer-Policy", "no-referrer")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.originalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }
}
