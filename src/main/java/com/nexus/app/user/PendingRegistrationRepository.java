package com.nexus.app.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {

    Optional<PendingRegistration> findByEmail(String email);

    boolean existsByEmail(String email);
}
