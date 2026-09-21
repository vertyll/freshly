package com.vertyll.freshly.auth.infrastructure.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.vertyll.freshly.auth.application.command.ChangePasswordCommand;

public record ChangePasswordRequestDto(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 12, max = 128) String newPassword
) {
    public ChangePasswordCommand toCommand(UUID userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }

    @Override
    public String toString() {
        return "ChangePasswordRequestDto[currentPassword=***, newPassword=***]";
    }
}
