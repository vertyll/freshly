package com.vertyll.freshly.useraccess.api.dto;

import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.common.enums.UserRoleEnum;

public record UserResponseDto(UUID id, boolean isActive, Set<UserRoleEnum> roles) {}
