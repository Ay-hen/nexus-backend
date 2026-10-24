package com.nexus.app.user;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
}
