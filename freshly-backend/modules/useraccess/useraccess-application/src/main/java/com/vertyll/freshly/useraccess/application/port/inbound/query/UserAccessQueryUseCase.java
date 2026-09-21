package com.vertyll.freshly.useraccess.application.port.inbound.query;

import java.util.Optional;
import java.util.UUID;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.useraccess.application.dto.UserResponse;

public interface UserAccessQueryUseCase {
    UserResponse getUser(UUID userId);

    Optional<UserResponse> findUser(UUID userId);

    PagedResponse<UserResponse> listUsers(PageRequest pageRequest);
}
