package com.clearnotes.dto.recording;

import java.math.BigDecimal;

public record TranscriptSegmentResponse(Long id, int segmentIndex, BigDecimal startSeconds,
                                        BigDecimal endSeconds, String text) { }
