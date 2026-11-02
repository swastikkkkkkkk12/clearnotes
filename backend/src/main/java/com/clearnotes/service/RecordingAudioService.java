package com.clearnotes.service;

import com.clearnotes.entity.Recording;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.RecordingRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class RecordingAudioService {
    private final RecordingRepository recordings;
    private final AudioStorageService storage;

    public RecordingAudioService(RecordingRepository recordings, AudioStorageService storage) {
        this.recordings = recordings;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public AudioFile getAudio(Long recordingId, Long userId) {
        Recording recording = recordings.findByIdAndUser_Id(recordingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found."));
        if (recording.isAudioDeleted())
            throw new ResourceNotFoundException("Audio was deleted after processing and is no longer available.");

        Path path = storage.pathFor(recording.getStoredFilename());
        if (!Files.isRegularFile(path) || !Files.isReadable(path))
            throw new ResourceNotFoundException("Audio file is no longer available.");

        Resource resource = new FileSystemResource(path);
        MediaType mediaType = Optional.ofNullable(recording.getContentType())
                .map(value -> {
                    try { return MediaType.parseMediaType(value); }
                    catch (IllegalArgumentException ignored) { return MediaType.APPLICATION_OCTET_STREAM; }
                })
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return new AudioFile(resource, mediaType, recording.getOriginalFilename());
    }

    public record AudioFile(Resource resource, MediaType contentType, String originalFilename) { }
}
