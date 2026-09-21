package com.vertyll.freshly.translation.application.port.inbound.command;

import com.vertyll.freshly.lang.i18n.TranslationMigration;

public interface TranslationMigrationUseCase {
    boolean runIfPending(TranslationMigration migration);
}
