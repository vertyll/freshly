package com.vertyll.freshly.translation.application.command;

import java.util.List;

public record ImportTranslationsCommand(List<ImportedTranslation> rows, String importedBy) {

    public ImportTranslationsCommand {
        rows = List.copyOf(rows);
    }
}
