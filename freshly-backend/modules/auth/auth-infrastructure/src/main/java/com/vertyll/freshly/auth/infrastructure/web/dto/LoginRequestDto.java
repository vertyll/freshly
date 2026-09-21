package com.vertyll.freshly.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String username, @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequestDto[username=" + username + ", password=***]";
    }
}
