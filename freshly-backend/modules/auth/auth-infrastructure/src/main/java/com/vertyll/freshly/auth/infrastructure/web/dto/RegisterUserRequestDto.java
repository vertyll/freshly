package com.vertyll.freshly.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.vertyll.freshly.auth.application.command.RegisterUserCommand;

public record RegisterUserRequestDto(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 128) String password,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName
) {
    public RegisterUserCommand toCommand(String languageTag) {
        return new RegisterUserCommand(username, email, password, firstName, lastName, languageTag);
    }

    @Override
    public String toString() {
        return "RegisterUserRequestDto[username=" + username + ", email=" + email + ", password=***, firstName="
                + firstName + ", lastName=" + lastName + "]";
    }
}
