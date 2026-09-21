package com.vertyll.freshly.translation.application.port.inbound.query;

import java.util.List;

import com.vertyll.freshly.translation.application.dto.TranslationEntry;

public interface TranslationExportUseCase {
    List<TranslationEntry> exportRows();
}
