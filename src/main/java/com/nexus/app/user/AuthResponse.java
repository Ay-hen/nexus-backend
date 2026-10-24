package com.nexus.app.user;

public record AuthResponse(
        String message,
        String token,
        String tokenType,
        String userId,
        String email,
        String role) {
}
