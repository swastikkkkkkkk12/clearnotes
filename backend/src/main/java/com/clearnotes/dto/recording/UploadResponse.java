package com.clearnotes.dto.recording;

import java.time.LocalDateTime;

public record UploadResponse(Long recordingId, Long jobId, String status, String originalFilename,
                             long fileSizeBytes, boolean deleteAudioAfterProcessing,
                             LocalDateTime createdAt) { }
