package com.vertyll.freshly.auth.application.command;

import java.util.UUID;

public record ChangeEmailCommand(UUID userId, String newEmail, String languageTag) {
}
