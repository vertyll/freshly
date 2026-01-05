package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequestDto(
        @NotBlank(message = "{validation.username.required}")
                @Size(min = 3, max = 50, message = "{validation.username.size}")
                String username,
        @NotBlank(message = "{validation.email.required}")
                @Email(message = "{validation.email.invalid}")
                String email,
        @NotBlank(message = "{validation.password.required}")
                @Size(min = 8, message = "{validation.password.minSize}")
                String password,
        @NotBlank(message = "{validation.firstName.required}") String firstName,
        @NotBlank(message = "{validation.lastName.required}") String lastName) {}
