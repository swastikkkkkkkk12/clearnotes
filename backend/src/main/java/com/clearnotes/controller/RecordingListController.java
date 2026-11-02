package com.clearnotes.controller;

import com.clearnotes.dto.recording.RecordingPageResponse;
import com.clearnotes.dto.recording.RecordingListItemResponse;
import com.clearnotes.repository.RecordingRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recordings")
public class RecordingListController {
    private final RecordingRepository recordings;

    public RecordingListController(RecordingRepository recordings) { this.recordings = recordings; }

    @GetMapping
    public RecordingPageResponse list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<RecordingListItemResponse> result = recordings.findPageForUser(
                Long.valueOf(jwt.getSubject()), PageRequest.of(page, size));
        return new RecordingPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
