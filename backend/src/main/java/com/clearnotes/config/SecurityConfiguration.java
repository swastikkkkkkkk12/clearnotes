package com.clearnotes.config;

import com.clearnotes.dto.ApiErrorResponse;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Base64;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    JwtEncoder jwtEncoder(@Value("${clearnotes.jwt.secret}") String secret) {
        SecretKey key = signingKey(secret);
        var jwk = new OctetSequenceKey.Builder(key).keyID("clearnotes-hs256").build();
        var jwkSource = new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${clearnotes.jwt.secret}") String secret,
                          @Value("${clearnotes.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(secret))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, error) ->
                writeSecurityError(mapper, request, response, 401, "UNAUTHORIZED", "A valid bearer token is required.");
        AccessDeniedHandler deniedHandler = (request, response, error) ->
                writeSecurityError(mapper, request, response, 403, "FORBIDDEN", "You are not allowed to access this resource.");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> { })
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .build();
    }

    private static SecretKey signingKey(String base64Secret) {
        byte[] bytes = Base64.getDecoder().decode(base64Secret);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static void writeSecurityError(ObjectMapper mapper, jakarta.servlet.http.HttpServletRequest request,
                                           jakarta.servlet.http.HttpServletResponse response, int status,
                                           String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(status, code, message, request.getRequestURI()));
    }
}
