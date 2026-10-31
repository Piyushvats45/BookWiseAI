package com.bookwise.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookwise.auth.AuthDtos.AuthResponse;
import com.bookwise.auth.AuthDtos.LoginRequest;
import com.bookwise.auth.AuthDtos.RegisterRequest;
import com.bookwise.auth.AuthDtos.UserDto;
import com.bookwise.common.ApiException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user = users.save(user);
        return response(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        return response(user);
    }

    public UserDto me(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists."));
        return toDto(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.issue(user), toDto(user));
    }

    private static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
