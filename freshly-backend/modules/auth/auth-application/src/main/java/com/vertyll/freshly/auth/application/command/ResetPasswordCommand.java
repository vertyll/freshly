package com.vertyll.freshly.auth.application.command;

public record ResetPasswordCommand(String token, String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordCommand[token=***, newPassword=***]";
    }
}
