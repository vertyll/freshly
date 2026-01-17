package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.SystemUserRepository;

@Repository("systemUserRepository")
@RequiredArgsConstructor
class MongoSystemUserRepository implements SystemUserRepository {
    private static final String CANNOT_UPDATE_NON_EXISTENT_USER =
            "Cannot update non-existent user: ";

    private final SpringDataSystemUserRepository springDataRepository;
    private final SystemUserMapper mapper;

    @Override
    public SystemUser save(SystemUser user) {
        SystemUserDocument document;

        if (user.getVersion() == null) {
            document = mapper.toDocument(user);
        } else {
            document =
                    springDataRepository
                            .findById(user.getKeycloakUserId())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    CANNOT_UPDATE_NON_EXISTENT_USER
                                                            + user.getKeycloakUserId()));

            document.setActive(user.isActive());
            document.setRoles(user.getRoles());
        }

        SystemUserDocument saved = springDataRepository.save(document);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SystemUser> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SystemUser> findByKeycloakUserId(UUID keycloakUserId) {
        return springDataRepository.findByKeycloakUserId(keycloakUserId).map(mapper::toDomain);
    }

    @Override
    public List<SystemUser> findAll() {
        return springDataRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(UUID keycloakUserId) {
        springDataRepository
                .findByKeycloakUserId(keycloakUserId)
                .ifPresent(springDataRepository::delete);
    }
}
