package com.nexus.app.user;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        @DefaultValue("PT5M") Duration ttl,
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("PT1M") Duration resendCooldown,
        @DefaultValue("5") int maxSendsPerHour,
        @DefaultValue("PT24H") Duration pendingRetention,
        String hmacSecret) {
}
