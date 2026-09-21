package com.vertyll.freshly.translation.application.port.inbound.command;

import com.vertyll.freshly.translation.application.command.OverrideTranslationCommand;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;

public interface TranslationAdminUseCase {
    TranslationEntry override(OverrideTranslationCommand command);

    TranslationEntry clearOverride(String key, String language);
}
