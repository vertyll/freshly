package com.vertyll.freshly.useraccess.application.service.query;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.useraccess.application.dto.UserResponse;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;
import com.vertyll.freshly.useraccess.domain.error.UserAccessError;
import com.vertyll.freshly.useraccess.domain.repository.SystemUserRepository;

public class UserAccessQueryService implements UserAccessQueryUseCase {
    private final SystemUserRepository users;

    public UserAccessQueryService(SystemUserRepository users) {
        this.users = users;
    }

    @Override
    public UserResponse getUser(UUID userId) {
        return findUser(userId)
            .orElseThrow(() -> new DomainException(UserAccessError.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    @Override
    public Optional<UserResponse> findUser(UUID userId) {
        return users.findByKeycloakUserId(userId).map(UserResponse::from);
    }

    @Override
    public PagedResponse<UserResponse> listUsers(PageRequest pageRequest) {
        return PagedResponse.from(users.findAll(pageRequest)).map(UserResponse::from);
    }

}
