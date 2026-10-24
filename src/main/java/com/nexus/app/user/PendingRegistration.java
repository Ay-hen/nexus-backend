package com.nexus.app.user;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "pending_registrations")
@Getter
@Setter
@NoArgsConstructor
public class PendingRegistration {

    @Id
    private String id;

    /** Normalized (trimmed, lower-case). Unique index created in MongoIndexInitializer. */
    private String email;

    private String name;

    private String passwordHash;

    /** HMAC of the current OTP; null means there is no live challenge. */
    private String otpHash;

    private Instant otpExpiresAt;

    private int failedAttempts;

    private Instant lastSentAt;

    private Instant sendWindowStart;

    private int sendCount;

    private Instant createdAt;

    /** Used only by the TTL index for cleanup; verification never relies on it. */
    private Instant expireAt;
}
