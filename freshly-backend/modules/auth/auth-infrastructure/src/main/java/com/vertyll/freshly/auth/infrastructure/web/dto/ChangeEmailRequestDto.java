package com.vertyll.freshly.auth.infrastructure.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.vertyll.freshly.auth.application.command.ChangeEmailCommand;

public record ChangeEmailRequestDto(@NotBlank @Email String newEmail) {

    public ChangeEmailCommand toCommand(UUID userId, String languageTag) {
        return new ChangeEmailCommand(userId, newEmail, languageTag);
    }
}
