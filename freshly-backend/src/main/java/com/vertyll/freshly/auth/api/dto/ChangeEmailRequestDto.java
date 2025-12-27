package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequestDto(
        @NotBlank(message = "New email is required")
        @Email(message = "Email must be valid")
        String newEmail
) {
}
