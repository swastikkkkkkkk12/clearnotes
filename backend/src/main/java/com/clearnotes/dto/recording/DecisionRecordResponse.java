package com.clearnotes.dto.recording;

import com.clearnotes.entity.FollowUpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DecisionRecordResponse(Long id, String decision, String approvedBy,
                                     Long sourceSegmentId, String evidence,
                                     BigDecimal startSeconds, BigDecimal endSeconds,
                                     FollowUpStatus followUpStatus, LocalDateTime createdAt) { }
