package com.nexus.app.user;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpHasher(OtpProperties properties) {
        String secret = properties.hmacSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.otp.hmac-secret (env OTP_HMAC_SECRET) must be set and at least 32 characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    /** HMAC-SHA256(secret, email + ":" + otp), hex encoded. Bound to the email. */
    public String hash(String email, String otp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((email + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC algorithm unavailable", ex);
        }
    }

    public boolean matches(String expectedHash, String email, String otp) {
        if (expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                hash(email, otp).getBytes(StandardCharsets.UTF_8));
    }
}
