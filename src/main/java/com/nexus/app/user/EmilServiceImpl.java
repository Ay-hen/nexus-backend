package com.nexus.app.user;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class EmilServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Nexus System - OTP Verification");
        message.setText("Your OTP code is: " + otp + "\n\nPlease use this code to verify your email address. This code will expire in 5 minutes.\n\nIf you did not request this code, please ignore this email.");
        mailSender.send(message);
    }
    
}
