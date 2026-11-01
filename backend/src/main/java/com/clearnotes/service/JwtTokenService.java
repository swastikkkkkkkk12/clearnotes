package com.clearnotes.service;

import com.clearnotes.dto.auth.AuthResponse;
import com.clearnotes.dto.auth.UserSummaryResponse;
import com.clearnotes.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttlSeconds;

    public JwtTokenService(JwtEncoder encoder,
                           @Value("${clearnotes.jwt.issuer}") String issuer,
                           @Value("${clearnotes.jwt.access-token-ttl-seconds}") long ttlSeconds) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public AuthResponse createFor(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ttlSeconds)).claim("email", user.getEmail()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).keyId("clearnotes-hs256").build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", ttlSeconds, new UserSummaryResponse(user.getId(), user.getEmail()));
    }
}
