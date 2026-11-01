package com.clearnotes.service;

import com.clearnotes.dto.recording.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioUploadService {
    private final AudioFileValidator validator;
    private final AudioStorageService storage;
    private final RecordingCreationService recordings;

    public AudioUploadService(AudioFileValidator validator, AudioStorageService storage,
                              RecordingCreationService recordings) {
        this.validator = validator;
        this.storage = storage;
        this.recordings = recordings;
    }

    public UploadResponse upload(Long userId, MultipartFile file, boolean deleteAfterProcessing) {
        AudioFileValidator.ValidatedAudio audio = validator.validate(file);
        String storedFilename = storage.store(file, audio.extension());
        try {
            return recordings.create(userId, audio, storedFilename, deleteAfterProcessing);
        } catch (RuntimeException failure) {
            storage.delete(storedFilename); // Compensate if the database transaction fails.
            throw failure;
        }
    }
}
