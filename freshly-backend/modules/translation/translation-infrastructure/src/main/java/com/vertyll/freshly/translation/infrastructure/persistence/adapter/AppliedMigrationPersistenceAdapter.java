package com.vertyll.freshly.translation.infrastructure.persistence.adapter;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.translation.domain.repository.AppliedMigrationRepository;
import com.vertyll.freshly.translation.infrastructure.persistence.document.AppliedMigrationDocument;
import com.vertyll.freshly.translation.infrastructure.persistence.repository.SpringDataAppliedMigrationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppliedMigrationPersistenceAdapter implements AppliedMigrationRepository {

    private final SpringDataAppliedMigrationRepository repository;

    @Override
    public boolean isApplied(String migrationId) {
        return repository.existsById(migrationId);
    }

    @Override
    public void markApplied(String migrationId, String context, Instant at) {
        repository.save(new AppliedMigrationDocument(migrationId, context, at));
    }
}
