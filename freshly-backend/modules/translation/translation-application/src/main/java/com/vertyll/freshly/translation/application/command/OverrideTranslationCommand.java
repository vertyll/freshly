package com.vertyll.freshly.translation.application.command;

import org.jspecify.annotations.Nullable;

public record OverrideTranslationCommand(
    String key,
    String language,
    String text,
    String author,
    @Nullable Long expectedVersion
) {
}
