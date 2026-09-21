package com.vertyll.freshly.translation.application.dto;

import java.util.Map;

public record TranslationBundle(String language, Map<String, String> messages, String etag) {
    public TranslationBundle {
        messages = Map.copyOf(messages);
    }
}
