package com.nexus.app.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/auth/otp")
public class OtpController {
    private final OtpService otpService;

    @PostMapping ("/send")
    public ResponseEntity<String> sendOtp(@RequestParam  String email) {
        otpService.generateAndSendOtp(email);
        return ResponseEntity.ok("OTP sent successfully.");
    }

    @PostMapping ("/verify")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        otpService.verifyOtp(email, otp);
        return ResponseEntity.ok("OTP verified successfully.");
    }
}
