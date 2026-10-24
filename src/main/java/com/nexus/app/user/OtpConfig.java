package com.nexus.app.user;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers OtpProperties. The Clock bean already lives in common/config/ClockConfig. */
@Configuration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpConfig {
}
