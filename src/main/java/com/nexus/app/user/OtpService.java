package com.nexus.app.user;

public interface OtpService {

    /** Creates or refreshes the pending registration and emails a new OTP. */
    void startRegistration(String email, String name, String passwordHash);

    /** Issues a fresh OTP for an existing pending registration (generic outcome if none). */
    void resend(String email);

    /**
     * Validates the OTP and atomically consumes (deletes) the pending registration.
     * Returns the consumed record; throws if verification fails.
     */
    PendingRegistration verify(String email, String otp);
}
