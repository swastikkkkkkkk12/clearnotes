package com.clearnotes.dto.recording;

import com.clearnotes.entity.FollowUpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDecisionRecordRequest(
        @NotBlank @Size(max = 10000) String decision,
        @Size(max = 255) String approvedBy,
        @NotNull FollowUpStatus followUpStatus) { }
