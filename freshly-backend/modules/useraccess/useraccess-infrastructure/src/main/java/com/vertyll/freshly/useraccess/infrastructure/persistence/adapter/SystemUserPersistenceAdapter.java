package com.vertyll.freshly.useraccess.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.useraccess.domain.model.SystemUser;
import com.vertyll.freshly.useraccess.domain.repository.SystemUserRepository;
import com.vertyll.freshly.useraccess.infrastructure.persistence.document.SystemUserDocument;
import com.vertyll.freshly.useraccess.infrastructure.persistence.repository.SpringDataSystemUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemUserPersistenceAdapter implements SystemUserRepository {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "keycloakUserId");

    private final SpringDataSystemUserRepository repository;

    @Override
    public SystemUser save(SystemUser user) {
        return toDomain(repository.save(toDocument(user)));
    }

    @Override
    public Optional<SystemUser> findByKeycloakUserId(UUID keycloakUserId) {
        return repository.findById(keycloakUserId).map(SystemUserPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByKeycloakUserId(UUID keycloakUserId) {
        return repository.existsById(keycloakUserId);
    }

    @Override
    public List<SystemUser> findAll() {
        return repository.findAll(DEFAULT_SORT).stream().map(SystemUserPersistenceAdapter::toDomain).toList();
    }

    @Override
    public PageResult<SystemUser> findAll(PageRequest pageRequest) {
        Page<SystemUserDocument> page = repository.findAll(
            org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), DEFAULT_SORT)
        );

        return new PageResult<>(
            page.getContent().stream().map(SystemUserPersistenceAdapter::toDomain).toList(),
            pageRequest.page(),
            pageRequest.size(),
            page.getTotalElements()
        );
    }

    @Override
    public void delete(UUID keycloakUserId) {
        repository.deleteById(keycloakUserId);
    }

    private static SystemUserDocument toDocument(SystemUser user) {
        return new SystemUserDocument(user.keycloakUserId(), user.isActive(), user.roles(), user.version());
    }

    private static SystemUser toDomain(SystemUserDocument document) {
        return SystemUser.reconstitute(
            document.getKeycloakUserId(),
            document.isActive(),
            document.getRoles(),
            document.getVersion()
        );
    }
}
