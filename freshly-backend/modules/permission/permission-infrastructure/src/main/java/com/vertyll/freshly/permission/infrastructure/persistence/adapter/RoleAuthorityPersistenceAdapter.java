package com.vertyll.freshly.permission.infrastructure.persistence.adapter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vertyll.freshly.permission.domain.model.RoleAuthority;
import com.vertyll.freshly.permission.domain.model.RoleGrants;
import com.vertyll.freshly.permission.domain.repository.RoleAuthorityRepository;
import com.vertyll.freshly.permission.infrastructure.persistence.document.RoleAuthorityDocument;
import com.vertyll.freshly.permission.infrastructure.persistence.repository.SpringDataRoleAuthorityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleAuthorityPersistenceAdapter implements RoleAuthorityRepository {
    public static final String GRANTS_CACHE = "permission-role-grants";

    private final SpringDataRoleAuthorityRepository repository;
    private final CacheManager caches;

    @Override
    public RoleAuthority save(RoleAuthority roleAuthority) {
        RoleAuthority saved = toDomain(repository.save(toDocument(roleAuthority)));
        evictAfterCommit();
        return saved;
    }

    @Override
    public Optional<RoleAuthority> findByRole(String role) {
        return repository.findById(role).map(RoleAuthorityPersistenceAdapter::toDomain);
    }

    @Override
    @Cacheable(value = GRANTS_CACHE, key = "#roles")
    public RoleGrants grantsFor(Set<String> roles) {
        if (roles.isEmpty()) {
            return RoleGrants.NOTHING;
        }

        List<RoleAuthorityDocument> found = repository.findAllById(roles);
        boolean anyUnrestricted = found.stream().anyMatch(RoleAuthorityDocument::unrestricted);

        Set<String> permissions = new LinkedHashSet<>();
        found.forEach(document -> permissions.addAll(document.permissions()));

        return new RoleGrants(anyUnrestricted, permissions);
    }

    @Override
    public List<RoleAuthority> findAll() {
        return repository.findAll().stream().map(RoleAuthorityPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void deleteByRole(String role) {
        repository.deleteById(role);
        evictAfterCommit();
    }

    private void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictNow();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictNow();
            }
        });
    }

    private void evictNow() {
        Cache cache = caches.getCache(GRANTS_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Cache " + GRANTS_CACHE + " is not registered");
        }
        cache.clear();
    }

    private static RoleAuthorityDocument toDocument(RoleAuthority authority) {
        return new RoleAuthorityDocument(
            authority.role(),
            authority.unrestricted(),
            authority.permissions(),
            authority.version()
        );
    }

    private static RoleAuthority toDomain(RoleAuthorityDocument document) {
        return RoleAuthority
            .reconstitute(document.role(), document.unrestricted(), document.permissions(), document.version());
    }
}
