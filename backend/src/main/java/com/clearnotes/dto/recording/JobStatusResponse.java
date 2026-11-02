package com.clearnotes.dto.recording;

import com.clearnotes.entity.JobStatus;

import java.time.LocalDateTime;

public record JobStatusResponse(Long jobId, Long recordingId, JobStatus status, int attemptCount,
                                String message, String errorMessage, LocalDateTime createdAt,
                                LocalDateTime startedAt, LocalDateTime completedAt) { }
