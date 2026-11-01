package com.clearnotes.service;

import com.clearnotes.dto.recording.UploadResponse;
import com.clearnotes.entity.AppUser;
import com.clearnotes.entity.ProcessingJob;
import com.clearnotes.entity.Recording;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.AppUserRepository;
import com.clearnotes.repository.ProcessingJobRepository;
import com.clearnotes.repository.RecordingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordingCreationService {
    private final AppUserRepository users;
    private final RecordingRepository recordings;
    private final ProcessingJobRepository jobs;

    public RecordingCreationService(AppUserRepository users, RecordingRepository recordings,
                                    ProcessingJobRepository jobs) {
        this.users = users;
        this.recordings = recordings;
        this.jobs = jobs;
    }

    @Transactional
    public UploadResponse create(Long userId, AudioFileValidator.ValidatedAudio audio,
                                 String storedFilename, boolean deleteAfterProcessing) {
        AppUser user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Recording recording = recordings.saveAndFlush(new Recording(user, audio.originalFilename(), storedFilename,
                audio.contentType(), audio.size(), deleteAfterProcessing));
        ProcessingJob job = jobs.save(new ProcessingJob(recording));
        return new UploadResponse(recording.getId(), job.getId(), job.getStatus().name(),
                recording.getOriginalFilename(), recording.getFileSizeBytes(),
                recording.isDeleteAudioAfterProcessing(), recording.getCreatedAt());
    }
}
