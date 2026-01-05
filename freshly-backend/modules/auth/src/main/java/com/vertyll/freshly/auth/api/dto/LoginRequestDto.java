package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "{validation.username.required}")
        String username,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}
