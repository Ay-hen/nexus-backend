package com.nexus.app.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nexus.app.common.error.TooManyRequestsException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final Duration SEND_WINDOW = Duration.ofHours(1);

    private final PendingRegistrationRepository pendingRepository;
    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;
    private final OtpHasher otpHasher;
    private final OtpProperties props;
    private final Clock clock;

    @Override
    public void startRegistration(String email, String name, String passwordHash) {
        String otp = otpHasher.generateOtp();
        String otpHash = otpHasher.hash(email, otp);
        Instant now = clock.instant();

        boolean issued;
        if (pendingRepository.existsByEmail(email)) {
            issued = claimSendSlot(email, now, otpHash, name, passwordHash);
        } else {
            try {
                pendingRepository.insert(newPending(email, name, passwordHash, otpHash, now));
                issued = true;
            } catch (DuplicateKeyException race) {
                // Another request created it between our check and insert.
                issued = claimSendSlot(email, now, otpHash, name, passwordHash);
            }
        }

        if (!issued) {
            throw tooManyRequests(email, now);
        }
        deliver(email, otp, otpHash);
    }

    @Override
    public void resend(String email) {
        String otp = otpHasher.generateOtp();
        String otpHash = otpHasher.hash(email, otp);
        Instant now = clock.instant();

        if (!claimSendSlot(email, now, otpHash, null, null)) {
            if (pendingRepository.existsByEmail(email)) {
                throw tooManyRequests(email, now);
            }
            return; // no pending registration: same generic outcome as success
        }
        deliver(email, otp, otpHash);
    }

    @Override
    public PendingRegistration verify(String email, String otp) {
        Instant now = clock.instant();

        // Reserve one attempt atomically. otpExpiresAt must be strictly in the future,
        // so a code is rejected at exactly its expiration time.
        Query attemptable = Query.query(Criteria.where("email").is(email)
                .and("otpHash").ne(null)
                .and("otpExpiresAt").gt(now)
                .and("failedAttempts").lt(props.maxFailedAttempts()));

        PendingRegistration pending = mongoTemplate.findAndModify(
                attemptable,
                new Update().inc("failedAttempts", 1),
                FindAndModifyOptions.options().returnNew(true),
                PendingRegistration.class);

        if (pending == null) {
            throw invalidCode();
        }

        if (!otpHasher.matches(pending.getOtpHash(), email, otp)) {
            if (pending.getFailedAttempts() >= props.maxFailedAttempts()) {
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("id").is(pending.getId())),
                        new Update().set("otpHash", null),
                        PendingRegistration.class);
                throw new TooManyRequestsException(
                        "Too many incorrect attempts. Request a new verification code.",
                        props.resendCooldown().toSeconds());
            }
            throw invalidCode();
        }

        // Consume: only one concurrent request can delete the record.
        PendingRegistration consumed = mongoTemplate.findAndRemove(
                Query.query(Criteria.where("id").is(pending.getId())
                        .and("otpHash").is(pending.getOtpHash())),
                PendingRegistration.class);

        if (consumed == null) {
            throw invalidCode();
        }
        return consumed;
    }

    // ---- helpers ----

    private PendingRegistration newPending(
            String email, String name, String passwordHash, String otpHash, Instant now) {
        PendingRegistration p = new PendingRegistration();
        p.setEmail(email);
        p.setName(name);
        p.setPasswordHash(passwordHash);
        p.setOtpHash(otpHash);
        p.setOtpExpiresAt(now.plus(props.ttl()));
        p.setFailedAttempts(0);
        p.setLastSentAt(now);
        p.setSendWindowStart(now);
        p.setSendCount(1);
        p.setCreatedAt(now);
        p.setExpireAt(now.plus(props.pendingRetention()));
        return p;
    }

    /**
     * Atomically claims a send slot (cooldown + hourly cap) and, in the same update,
     * replaces the OTP, which invalidates the previous one. Returns false if no
     * document matched (missing, in cooldown, or hourly cap reached).
     */
    private boolean claimSendSlot(
            String email, Instant now, String otpHash, String name, String passwordHash) {

        Instant cooldownEdge = now.minus(props.resendCooldown());
        Instant windowEdge = now.minus(SEND_WINDOW);

        Query windowExpired = Query.query(Criteria.where("email").is(email)
                .and("lastSentAt").lte(cooldownEdge)
                .and("sendWindowStart").lte(windowEdge));
        Update restartWindow = challengeUpdate(now, otpHash, name, passwordHash)
                .set("sendWindowStart", now)
                .set("sendCount", 1);
        if (mongoTemplate.findAndModify(windowExpired, restartWindow, PendingRegistration.class) != null) {
            return true;
        }

        Query windowActive = Query.query(Criteria.where("email").is(email)
                .and("lastSentAt").lte(cooldownEdge)
                .and("sendWindowStart").gt(windowEdge)
                .and("sendCount").lt(props.maxSendsPerHour()));
        Update countSend = challengeUpdate(now, otpHash, name, passwordHash).inc("sendCount", 1);
        return mongoTemplate.findAndModify(windowActive, countSend, PendingRegistration.class) != null;
    }

    private Update challengeUpdate(Instant now, String otpHash, String name, String passwordHash) {
        Update update = new Update()
                .set("otpHash", otpHash)
                .set("otpExpiresAt", now.plus(props.ttl()))
                .set("failedAttempts", 0)
                .set("lastSentAt", now)
                .set("expireAt", now.plus(props.pendingRetention()));
        if (name != null) {
            update.set("name", name);
        }
        if (passwordHash != null) {
            update.set("passwordHash", passwordHash);
        }
        return update;
    }

    private void deliver(String email, String otp, String otpHash) {
        try {
            emailService.sendOtpEmail(email, otp);
        } catch (MailException ex) {
            // Never log the OTP, the address or the exception message.
            log.warn("OTP email delivery failed ({})", ex.getClass().getSimpleName());
            // Drop the undelivered code and release the cooldown so the user can retry.
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("email").is(email).and("otpHash").is(otpHash)),
                    new Update().set("otpHash", null).set("lastSentAt", Instant.EPOCH),
                    PendingRegistration.class);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification email. Please try again shortly.");
        }
    }

    private TooManyRequestsException tooManyRequests(String email, Instant now) {
        long retryAfter = pendingRepository.findByEmail(email)
                .map(p -> {
                    Instant cooldownEnds = p.getLastSentAt().plus(props.resendCooldown());
                    Instant windowEnds = p.getSendWindowStart().plus(SEND_WINDOW);
                    boolean capped = p.getSendCount() >= props.maxSendsPerHour() && windowEnds.isAfter(now);
                    Instant next = capped && windowEnds.isAfter(cooldownEnds) ? windowEnds : cooldownEnds;
                    return Duration.between(now, next).toSeconds();
                })
                .orElse(props.resendCooldown().toSeconds());
        return new TooManyRequestsException(
                "Too many verification emails requested. Please wait before trying again.",
                Math.max(1, retryAfter));
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
    }
}
