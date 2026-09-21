package com.vertyll.freshly.permission.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vertyll.freshly.permission.domain.model.RoleAuthority;
import com.vertyll.freshly.permission.domain.model.RoleGrants;

public interface RoleAuthorityRepository {
    RoleAuthority save(RoleAuthority roleAuthority);

    Optional<RoleAuthority> findByRole(String role);

    RoleGrants grantsFor(Set<String> roles);

    List<RoleAuthority> findAll();

    void deleteByRole(String role);
}
