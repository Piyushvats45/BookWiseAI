package com.bookwise.auth;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated user's id is the JWT subject. Always scope data access by this id. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
