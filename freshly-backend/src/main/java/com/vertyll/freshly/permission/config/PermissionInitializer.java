package com.vertyll.freshly.permission.config;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class PermissionInitializer {

    private final RolePermissionMappingRepository repository;

    @Bean
    public CommandLineRunner initializePermissions() {
        return _ -> {
            log.info("Initializing default role-permission mappings...");

            // USER role permissions
            createMappingIfNotExists("USER", Permission.AUTH_CHANGE_PASSWORD);
            createMappingIfNotExists("USER", Permission.AUTH_CHANGE_EMAIL);
            createMappingIfNotExists("USER", Permission.USERS_READ);

            // ADMIN role permissions (all permissions)
            for (Permission permission : Permission.values()) {
                createMappingIfNotExists("ADMIN", permission);
            }

            log.info("Role-permission mappings initialized successfully");
        };
    }

    private void createMappingIfNotExists(String role, Permission permission) {
        if (!repository.existsByKeycloakRoleAndPermission(role, permission)) {
            RolePermissionMapping mapping = new RolePermissionMapping(role, permission);
            repository.save(mapping);
            log.debug("Created mapping: {} -> {}", role, permission.getValue());
        }
    }
}
