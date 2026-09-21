package com.vertyll.freshly.useraccess.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.useraccess.domain.model.SystemUser;
import com.vertyll.freshly.useraccess.domain.repository.SystemUserRepository;

public class InMemorySystemUserRepository implements SystemUserRepository {
    private final Map<UUID, SystemUser> stored = new LinkedHashMap<>();

    private int saveCount;

    @Override
    public SystemUser save(SystemUser user) {
        saveCount++;
        stored.put(user.keycloakUserId(), user);
        return user;
    }

    @Override
    public Optional<SystemUser> findByKeycloakUserId(UUID keycloakUserId) {
        return Optional.ofNullable(stored.get(keycloakUserId));
    }

    @Override
    public boolean existsByKeycloakUserId(UUID keycloakUserId) {
        return stored.containsKey(keycloakUserId);
    }

    @Override
    public List<SystemUser> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public PageResult<SystemUser> findAll(PageRequest pageRequest) {
        List<SystemUser> all = findAll();
        int from = Math.min(pageRequest.page() * pageRequest.size(), all.size());
        int to = Math.min(from + pageRequest.size(), all.size());

        return new PageResult<>(all.subList(from, to), pageRequest.page(), pageRequest.size(), all.size());
    }

    @Override
    public void delete(UUID keycloakUserId) {
        stored.remove(keycloakUserId);
    }

    public int saveCount() {
        return saveCount;
    }

    public void seed(SystemUser user) {
        stored.put(user.keycloakUserId(), user);
    }
}
