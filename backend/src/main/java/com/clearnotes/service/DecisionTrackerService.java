package com.clearnotes.service;

import com.clearnotes.dto.recording.DecisionRecordResponse;
import com.clearnotes.dto.recording.UpdateDecisionRecordRequest;
import com.clearnotes.entity.DecisionRecord;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.DecisionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DecisionTrackerService {
    private final DecisionRecordRepository decisions;

    public DecisionTrackerService(DecisionRecordRepository decisions) {
        this.decisions = decisions;
    }

    @Transactional(readOnly = true)
    public List<DecisionRecordResponse> getDecisions(Long recordingId, Long userId) {
        return decisions.findAllByRecording_IdAndRecording_User_IdOrderByCreatedAtAscIdAsc(recordingId, userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DecisionRecordResponse updateDecision(Long recordingId, Long decisionId, Long userId,
                                                  UpdateDecisionRecordRequest request) {
        DecisionRecord decision = decisions.findByIdAndRecording_IdAndRecording_User_Id(
                        decisionId, recordingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision not found."));
        decision.setDecision(request.decision().trim());
        decision.setApprovedBy(clean(request.approvedBy()));
        decision.setFollowUpStatus(request.followUpStatus());
        return toResponse(decision);
    }

    private DecisionRecordResponse toResponse(DecisionRecord decision) {
        var source = decision.getSourceSegment();
        return new DecisionRecordResponse(decision.getId(), decision.getDecision(), decision.getApprovedBy(),
                source.getId(), source.getText(), source.getStartSeconds(), source.getEndSeconds(),
                decision.getFollowUpStatus(), decision.getCreatedAt());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
