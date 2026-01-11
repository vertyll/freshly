package com.vertyll.freshly.useraccess.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.SystemUserRepository;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import com.vertyll.freshly.useraccess.domain.exception.UserAlreadyExistsException;
import com.vertyll.freshly.useraccess.domain.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccessService {

    private final SystemUserRepository systemUserRepository;

    public SystemUser createUser(UUID keycloakUserId, boolean isActive, Set<UserRoleEnum> roles) {
        if (systemUserRepository.findById(keycloakUserId).isPresent()) {
            throw new UserAlreadyExistsException(keycloakUserId);
        }

        SystemUser user = new SystemUser(keycloakUserId, isActive, roles);
        SystemUser savedUser = systemUserRepository.save(user);

        log.info("User {} created with roles {}", keycloakUserId, roles);
        return savedUser;
    }

    public SystemUser getUserById(UUID userId) {
        log.debug("Fetching user by id: {}", userId);
        return systemUserRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public List<SystemUser> getAllUsers() {
        log.debug("Fetching all users");
        return systemUserRepository.findAll();
    }

    public void activateUser(UUID userId) {
        SystemUser user =
                systemUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        user.activate();
        systemUserRepository.save(user);

        log.info("User {} activated", userId);
    }

    public void deactivateUser(UUID userId, UUID loggedInUserId) {
        SystemUser user =
                systemUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        user.deactivateSelf();
        systemUserRepository.save(user);

        log.info("User {} deactivated by {}", userId, loggedInUserId);
    }

    public void replaceUserRoles(UUID userId, Set<UserRoleEnum> roles) {
        SystemUser user =
                systemUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        user.replaceRoles(roles);
        systemUserRepository.save(user);

        log.info("Roles for user {} replaced with {}", userId, roles);
    }
}
