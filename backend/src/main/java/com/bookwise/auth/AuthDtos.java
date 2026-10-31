package com.bookwise.auth;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt only uses the first 72 bytes, so cap it there.
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserDto(UUID id, String email, String displayName) {
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
