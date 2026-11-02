package com.clearnotes.service;

import com.clearnotes.dto.recording.JobStatusResponse;
import com.clearnotes.entity.JobStatus;
import com.clearnotes.entity.ProcessingJob;
import com.clearnotes.entity.Recording;
import com.clearnotes.entity.TranscriptSegment;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.ProcessingJobRepository;
import com.clearnotes.repository.RecordingRepository;
import com.clearnotes.repository.TranscriptSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcessingJobService {
    private static final String FAILURE_MESSAGE = "Audio processing failed. Please try again.";
    private final ProcessingJobRepository jobs;
    private final RecordingRepository recordings;
    private final TranscriptSegmentRepository segments;

    public ProcessingJobService(ProcessingJobRepository jobs, RecordingRepository recordings,
                                TranscriptSegmentRepository segments) {
        this.jobs = jobs;
        this.recordings = recordings;
        this.segments = segments;
    }

    @Transactional
    public ProcessingInput beginTranscription(Long jobId) {
        ProcessingJob job = findJob(jobId);
        if (job.getStatus() != JobStatus.QUEUED) return null;
        job.setStatus(JobStatus.TRANSCRIBING);
        job.setStartedAt(LocalDateTime.now());
        job.setCompletedAt(null);
        job.setErrorMessage(null);
        job.setAttemptCount(job.getAttemptCount() + 1);
        Recording recording = job.getRecording();
        return new ProcessingInput(recording.getId(), recording.getStoredFilename(),
                recording.isDeleteAudioAfterProcessing());
    }

    @Transactional
    public void saveTranscriptAndEnterExtraction(Long jobId, List<TranscriptionClient.TranscriptPart> parts) {
        ProcessingJob job = findJob(jobId);
        if (job.getStatus() != JobStatus.TRANSCRIBING)
            throw new IllegalStateException("Job is not currently transcribing.");
        Recording recording = job.getRecording();
        segments.deleteAllByRecording_Id(recording.getId());
        for (int index = 0; index < parts.size(); index++) {
            TranscriptionClient.TranscriptPart part = parts.get(index);
            if (part.text() == null || part.text().isBlank() || !Double.isFinite(part.start())
                    || !Double.isFinite(part.end()) || part.start() < 0 || part.end() < part.start())
                throw new IllegalArgumentException("Transcription service returned an invalid segment.");
            segments.save(new TranscriptSegment(recording, index, seconds(part.start()), seconds(part.end()),
                    part.text().trim()));
        }
        job.setStatus(JobStatus.EXTRACTING);
    }

    @Transactional
    public void complete(Long jobId) {
        ProcessingJob job = findJob(jobId);
        if (job.getStatus() != JobStatus.EXTRACTING)
            throw new IllegalStateException("Job is not ready to complete.");
        job.setStatus(JobStatus.DONE);
        job.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public void fail(Long jobId) {
        ProcessingJob job = findJob(jobId);
        if (job.getStatus() == JobStatus.DONE) return;
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(FAILURE_MESSAGE);
        job.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public void markAudioDeleted(Long recordingId) {
        Recording recording = recordings.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found."));
        recording.setAudioDeleted(true);
    }

    @Transactional(readOnly = true)
    public JobStatusResponse getStatus(Long jobId, Long userId) {
        ProcessingJob job = jobs.findByIdAndRecording_User_Id(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));
        return new JobStatusResponse(job.getId(), job.getRecording().getId(), job.getStatus(),
                job.getAttemptCount(), statusMessage(job.getStatus()), job.getErrorMessage(),
                job.getCreatedAt(), job.getStartedAt(), job.getCompletedAt());
    }

    private ProcessingJob findJob(Long jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found."));
    }

    private static BigDecimal seconds(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }

    private static String statusMessage(JobStatus status) {
        return switch (status) {
            case QUEUED -> "Waiting to start.";
            case TRANSCRIBING -> "Converting audio to text.";
            case EXTRACTING -> "Extracting decisions, tasks, and meeting details.";
            case DONE -> "Transcript and notes are ready.";
            case FAILED -> FAILURE_MESSAGE;
        };
    }

    public record ProcessingInput(Long recordingId, String storedFilename, boolean deleteAudioAfterProcessing) { }
}
