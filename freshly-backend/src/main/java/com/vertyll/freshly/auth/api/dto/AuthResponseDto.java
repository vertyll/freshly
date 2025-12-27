package com.vertyll.freshly.auth.api.dto;

import java.util.UUID;

public record AuthResponseDto(
        UUID userId,
        String message
) {
}
