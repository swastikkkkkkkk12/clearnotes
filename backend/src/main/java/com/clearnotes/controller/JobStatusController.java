package com.clearnotes.controller;

import com.clearnotes.dto.recording.JobStatusResponse;
import com.clearnotes.service.ProcessingJobService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobStatusController {
    private final ProcessingJobService jobs;

    public JobStatusController(ProcessingJobService jobs) { this.jobs = jobs; }

    @GetMapping("/{jobId}")
    public JobStatusResponse getStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId) {
        return jobs.getStatus(jobId, Long.valueOf(jwt.getSubject()));
    }
}
