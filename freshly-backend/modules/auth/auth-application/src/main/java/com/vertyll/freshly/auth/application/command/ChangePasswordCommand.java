package com.vertyll.freshly.auth.application.command;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, String currentPassword, String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordCommand[userId=" + userId + ", currentPassword=***, newPassword=***]";
    }
}
