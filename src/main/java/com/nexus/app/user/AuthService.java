package com.nexus.app.user;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nexus.app.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final OtpProperties otpProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            OtpProperties otpProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.otpProperties = otpProperties;
    }

    public RegistrationResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        otpService.startRegistration(
                email,
                request.getName().trim(),
                passwordEncoder.encode(request.getPassword()));

        return new RegistrationResponse(
                "Verification required. We sent a 6-digit code to your email.",
                email,
                true,
                otpProperties.ttl().toSeconds());
    }

    public AuthResponse verifyOtp(String email, String otp) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        PendingRegistration pending = otpService.verify(normalizedEmail, otp);

        // 3-arg constructor always assigns the default role "USER".
        User user = new User(pending.getName(), pending.getEmail(), pending.getPasswordHash());
        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(
                "Email verified successfully",
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole());
    }

    public MessageResponse resendOtp(String email) {
        otpService.resend(EmailNormalizer.normalize(email));
        return new MessageResponse(
                "If a pending registration exists for this email, a new code has been sent.");
    }

    public AuthResponse login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(
                "Login successful",
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole());
    }
}
