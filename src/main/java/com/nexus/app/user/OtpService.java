package com.nexus.app.user;

public interface OtpService {
    void generateAndSendOtp(String email);
    void verifyOtp(String email, String otp);
}
