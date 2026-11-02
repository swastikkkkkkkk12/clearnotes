package com.clearnotes.dto.recording;

import java.time.LocalDateTime;

public record AudioPlaybackLinkResponse(String url, LocalDateTime expiresAt) { }
