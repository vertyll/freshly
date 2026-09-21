package com.vertyll.freshly.translation.application.command;

public record ImportedTranslation(String key, String language, String text, int rowNumber) {
}
