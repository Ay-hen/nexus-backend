package com.nexus.app.user;

public record RegistrationResponse(
        String message,
        String email,
        boolean verificationRequired,
        long otpExpiresInSeconds) {
}
