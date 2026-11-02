package com.clearnotes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalLong;

@Service
public class AudioPlaybackLinkService {
    private static final long LINK_TTL_SECONDS = 900;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final String issuer;

    public AudioPlaybackLinkService(JwtEncoder encoder, JwtDecoder decoder,
                                    @Value("${clearnotes.jwt.issuer}") String issuer) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.issuer = issuer;
    }

    public AudioPlaybackLink create(Long recordingId, Long userId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(LINK_TTL_SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
                .issuedAt(now).expiresAt(expires).claim("purpose", "audio-playback")
                .claim("recordingId", recordingId).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).keyId("clearnotes-hs256").build(), claims)).getTokenValue();
        String url = "/api/media/audio/" + recordingId + "?token=" + token;
        return new AudioPlaybackLink(url, LocalDateTime.ofInstant(expires, ZoneOffset.UTC));
    }

    public OptionalLong userIdFor(String token, Long recordingId) {
        try {
            var jwt = decoder.decode(token);
            Object tokenRecordingId = jwt.getClaims().get("recordingId");
            if (!"audio-playback".equals(jwt.getClaimAsString("purpose"))
                    || tokenRecordingId == null
                    || Long.parseLong(tokenRecordingId.toString()) != recordingId)
                return OptionalLong.empty();
            return OptionalLong.of(Long.parseLong(jwt.getSubject()));
        } catch (JwtException | NumberFormatException | NullPointerException invalidToken) {
            return OptionalLong.empty();
        }
    }

    public record AudioPlaybackLink(String url, LocalDateTime expiresAt) { }
}
