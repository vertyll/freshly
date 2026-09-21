package com.vertyll.freshly.translation.application.service.query;

import java.util.Comparator;
import java.util.List;

import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationExportUseCase;
import com.vertyll.freshly.translation.domain.model.SupportedLanguage;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationExportService implements TranslationExportUseCase {
    private final TranslationRepository translations;

    public TranslationExportService(TranslationRepository translations) {
        this.translations = translations;
    }

    @Override
    public List<TranslationEntry> exportRows() {
        List<String> languages = SupportedLanguage.tags();

        return translations.findAll()
            .stream()
            .sorted(Comparator.comparing(TranslationKey::context).thenComparing(TranslationKey::key))
            .map(key -> TranslationEntry.from(key, languages))
            .toList();
    }
}
