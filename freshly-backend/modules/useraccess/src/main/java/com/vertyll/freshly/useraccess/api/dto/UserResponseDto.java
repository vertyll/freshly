package com.vertyll.freshly.useraccess.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponseDto(UUID id, boolean isActive, Set<String> roles) {}
