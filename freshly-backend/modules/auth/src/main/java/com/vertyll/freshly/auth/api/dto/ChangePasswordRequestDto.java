package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank(message = "{validation.password.current.required}") String currentPassword,
        @NotBlank(message = "{validation.password.new.required}")
                @Size(min = 8, message = "{validation.password.minSize}")
                String newPassword) {}
