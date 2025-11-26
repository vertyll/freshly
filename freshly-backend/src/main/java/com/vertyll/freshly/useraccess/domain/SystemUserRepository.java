package com.vertyll.freshly.useraccess.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemUserRepository {
    SystemUser save(SystemUser user);

    Optional<SystemUser> findById(UUID id);

    List<SystemUser> findAll();
}
