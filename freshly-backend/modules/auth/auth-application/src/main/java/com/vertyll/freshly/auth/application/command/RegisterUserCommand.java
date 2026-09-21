package com.vertyll.freshly.auth.application.command;

public record RegisterUserCommand(
    String username,
    String email,
    String password,
    String firstName,
    String lastName,
    String languageTag
) {

    @Override
    public String toString() {
        return "RegisterUserCommand[username=" + username + ", email=" + email + ", password=***, firstName="
                + firstName + ", lastName=" + lastName + ", languageTag=" + languageTag + "]";
    }
}
