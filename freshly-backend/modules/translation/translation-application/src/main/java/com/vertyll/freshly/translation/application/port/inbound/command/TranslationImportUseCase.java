package com.vertyll.freshly.translation.application.port.inbound.command;

import com.vertyll.freshly.translation.application.command.ImportTranslationsCommand;
import com.vertyll.freshly.translation.application.dto.ImportReport;

public interface TranslationImportUseCase {
    ImportReport importOverrides(ImportTranslationsCommand command);
}
