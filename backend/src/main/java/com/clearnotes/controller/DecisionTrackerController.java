package com.clearnotes.controller;

import com.clearnotes.dto.recording.DecisionRecordResponse;
import com.clearnotes.dto.recording.UpdateDecisionRecordRequest;
import com.clearnotes.service.DecisionTrackerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recordings/{recordingId}/decisions")
public class DecisionTrackerController {
    private final DecisionTrackerService decisions;

    public DecisionTrackerController(DecisionTrackerService decisions) {
        this.decisions = decisions;
    }

    @GetMapping
    public List<DecisionRecordResponse> getDecisions(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long recordingId) {
        return decisions.getDecisions(recordingId, Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/{decisionId}")
    public DecisionRecordResponse updateDecision(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long recordingId,
                                                  @PathVariable Long decisionId,
                                                  @Valid @RequestBody UpdateDecisionRecordRequest request) {
        return decisions.updateDecision(recordingId, decisionId, Long.valueOf(jwt.getSubject()), request);
    }
}
