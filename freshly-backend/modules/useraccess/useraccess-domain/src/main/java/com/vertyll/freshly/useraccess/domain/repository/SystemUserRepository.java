package com.vertyll.freshly.useraccess.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.useraccess.domain.model.SystemUser;

public interface SystemUserRepository {
    SystemUser save(SystemUser user);

    Optional<SystemUser> findByKeycloakUserId(UUID keycloakUserId);

    boolean existsByKeycloakUserId(UUID keycloakUserId);

    List<SystemUser> findAll();

    PageResult<SystemUser> findAll(PageRequest pageRequest);

    void delete(UUID keycloakUserId);
}
