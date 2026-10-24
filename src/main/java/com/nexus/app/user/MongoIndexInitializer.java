package com.nexus.app.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Creates required indexes explicitly instead of relying on auto-index-creation.
 * Fails fast at startup if the unique index cannot be built (e.g. duplicate emails exist).
 */
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations users = mongoTemplate.indexOps(User.class);
        boolean hasUniqueEmail = users.getIndexInfo().stream().anyMatch(info ->
                info.isUnique()
                        && info.getIndexFields().size() == 1
                        && "email".equals(info.getIndexFields().get(0).getKey()));
        if (!hasUniqueEmail) {
            users.createIndex(new Index().on("email", Sort.Direction.ASC).unique().named("uk_users_email"));
        }

        IndexOperations pending = mongoTemplate.indexOps(PendingRegistration.class);
        pending.createIndex(new Index().on("email", Sort.Direction.ASC).unique().named("uk_pending_email"));
        pending.createIndex(new Index().on("expireAt", Sort.Direction.ASC).expire(0L).named("ttl_pending_expire_at"));
    }
}
