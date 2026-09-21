package com.vertyll.freshly.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.vertyll.freshly.auth.application.command.ResetPasswordCommand;

public record ResetPasswordRequestDto(@NotBlank String token, @NotBlank @Size(min = 12, max = 128) String newPassword) {

    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(token, newPassword);
    }

    @Override
    public String toString() {
        return "ResetPasswordRequestDto[token=***, newPassword=***]";
    }
}
