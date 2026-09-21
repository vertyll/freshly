package com.vertyll.freshly.translation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.translation.application.command.OverrideTranslationCommand;

public record OverrideTranslationRequestDto(@NotBlank @Size(max = 4000) String text) {
    public OverrideTranslationCommand toCommand(
        String key,
        String language,
        String author,
        @Nullable Long expectedVersion
    ) {
        return new OverrideTranslationCommand(key, language, text, author, expectedVersion);
    }
}
