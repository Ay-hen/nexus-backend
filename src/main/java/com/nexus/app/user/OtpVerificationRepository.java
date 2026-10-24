package com.nexus.app.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OtpVerificationRepository extends MongoRepository<OtpVerification, String> {
    Optional<OtpVerification> findTopByEmailOrderByExpirationTimeDesc(String email);
}
