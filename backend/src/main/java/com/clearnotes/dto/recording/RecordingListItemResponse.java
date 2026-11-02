package com.clearnotes.dto.recording;

import com.clearnotes.entity.JobStatus;

import java.time.LocalDateTime;

public record RecordingListItemResponse(Long id, String originalFilename, long fileSizeBytes,
                                        LocalDateTime createdAt, boolean audioDeleted,
                                        Long jobId, JobStatus status) { }
