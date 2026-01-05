package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequestDto(
        @NotBlank(message = "{validation.email.new.required}")
                @Email(message = "{validation.email.invalid}")
                String newEmail) {}
