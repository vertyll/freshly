package com.vertyll.freshly.translation.domain.repository;

import java.time.Instant;

public interface AppliedMigrationRepository {
    boolean isApplied(String migrationId);

    void markApplied(String migrationId, String context, Instant at);
}
