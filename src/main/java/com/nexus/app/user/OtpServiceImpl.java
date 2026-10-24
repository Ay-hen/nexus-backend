package com.nexus.app.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor    
public class OtpServiceImpl implements OtpService {

    private final EmailService emailService;

    private final OtpVerificationRepository otpVerificationRepo;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setEmail(email);
        otpVerification.setOtp(otp);
        otpVerification.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        otpVerification.setVerified(false);
        otpVerificationRepo.save(otpVerification);
        emailService.sendOtpEmail(email, otp);
    }

    @Override
    public void verifyOtp(String email, String otp) {
        OtpVerification otpVerification = otpVerificationRepo.findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new RuntimeException("No OTP found for the provided email."));

        if (otpVerification.isVerified()) {
            throw new RuntimeException("OTP has already been verified.");
        }

        if (otpVerification.getExpirationTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired.");
        }

        if (!otpVerification.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP.");
        }

        otpVerification.setVerified(true);
        otpVerificationRepo.save(otpVerification);
    }
}
