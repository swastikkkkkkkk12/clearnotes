package com.clearnotes.service;

import com.clearnotes.dto.auth.AuthResponse;
import com.clearnotes.dto.auth.LoginRequest;
import com.clearnotes.dto.auth.RegisterRequest;
import com.clearnotes.dto.auth.UserSummaryResponse;
import com.clearnotes.entity.AppUser;
import com.clearnotes.exception.EmailAlreadyRegisteredException;
import com.clearnotes.exception.InvalidPasswordException;
import com.clearnotes.exception.ResourceNotFoundException;
import com.clearnotes.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        ensureBcryptLength(request.password(), true);
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw new EmailAlreadyRegisteredException();
        AppUser user = users.saveAndFlush(new AppUser(email, passwordEncoder.encode(request.password())));
        return tokens.createFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        ensureBcryptLength(request.password(), false);
        AppUser user = users.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password.");
        return tokens.createFor(user);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse currentUser(Long userId) {
        AppUser user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserSummaryResponse(user.getId(), user.getEmail());
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private static void ensureBcryptLength(String password, boolean registration) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            if (registration) throw new InvalidPasswordException();
            throw new BadCredentialsException("Invalid email or password.");
        }
    }
}
