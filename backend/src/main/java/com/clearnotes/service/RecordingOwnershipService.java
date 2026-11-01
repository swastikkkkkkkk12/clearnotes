package com.clearnotes.service;

import com.clearnotes.entity.Recording;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.RecordingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordingOwnershipService {
    private final RecordingRepository recordings;

    public RecordingOwnershipService(RecordingRepository recordings) { this.recordings = recordings; }

    @Transactional(readOnly = true)
    public Recording requireOwnedByCurrentUser(Long recordingId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt))
            throw new AccessDeniedException("Authentication is required.");
        Long userId = Long.valueOf(jwt.getSubject());
        return recordings.findByIdAndUser_Id(recordingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found."));
    }
}
