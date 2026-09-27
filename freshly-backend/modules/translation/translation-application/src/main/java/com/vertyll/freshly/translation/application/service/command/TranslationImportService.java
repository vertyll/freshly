package com.vertyll.freshly.translation.application.service.command;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.translation.application.command.ImportTranslationsCommand;
import com.vertyll.freshly.translation.application.command.ImportedTranslation;
import com.vertyll.freshly.translation.application.dto.ImportReport;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationImportUseCase;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.SupportedLanguage;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationImportService implements TranslationImportUseCase {
    private final TranslationRepository translations;
    private final MessageGrammar grammar;
    private final UseCaseLogger logger;

    public TranslationImportService(TranslationRepository translations, MessageGrammar grammar, UseCaseLogger logger) {
        this.translations = translations;
        this.grammar = grammar;
        this.logger = logger;
    }

    @Override
    public ImportReport importOverrides(ImportTranslationsCommand command) {
        Set<String> languages = Set.copyOf(SupportedLanguage.tags());
        Map<String, TranslationKey> known = translations
            .findAllByKeys(command.rows().stream().map(ImportedTranslation::key).collect(Collectors.toSet()));

        Set<String> unknownKeys = new TreeSet<>();
        Set<String> unknownLanguages = new TreeSet<>();
        List<ImportReport.RejectedRow> rejected = new ArrayList<>();
        Map<String, TranslationKey> touched = new LinkedHashMap<>();

        Map<Outcome, Integer> outcomes = new EnumMap<>(Outcome.class);

        for (ImportedTranslation row : command.rows()) {
            TranslationKey key = known.get(row.key());
            if (key == null) {
                unknownKeys.add(row.key());
            } else if (languages.contains(row.language())) {
                Outcome outcome = apply(key, row, command.importedBy(), rejected);
                outcomes.merge(outcome, 1, Integer::sum);
                if (outcome == Outcome.APPLIED || outcome == Outcome.CLEARED) {
                    touched.put(key.key(), key);
                }
            } else {
                unknownLanguages.add(row.language());
            }
        }

        int applied = outcomes.getOrDefault(Outcome.APPLIED, 0);
        int cleared = outcomes.getOrDefault(Outcome.CLEARED, 0);
        int unchanged = outcomes.getOrDefault(Outcome.UNCHANGED, 0);

        translations.saveAll(touched.values());
        logger.info(
            "Import by {}: {} applied, {} cleared, {} unchanged, {} rejected, {} unknown keys",
            command.importedBy(),
            applied,
            cleared,
            unchanged,
            rejected.size(),
            unknownKeys.size()
        );

        return new ImportReport(
            applied,
            cleared,
            unchanged,
            List.copyOf(unknownKeys),
            List.copyOf(unknownLanguages),
            rejected,
            missingAfterImport(languages)
        );
    }

    private Outcome apply(
        TranslationKey key,
        ImportedTranslation row,
        String importedBy,
        List<ImportReport.RejectedRow> rejected
    ) {
        String currentDefault = key.defaults().get(row.language());

        if (row.text().equals(currentDefault)) {
            if (!key.overrides().containsKey(row.language())) {
                return Outcome.UNCHANGED;
            }
            key.clearOverride(row.language());
            return Outcome.CLEARED;
        }

        if (row.text().equals(textOf(key, row.language()))) {
            return Outcome.UNCHANGED;
        }

        try {
            key.override(row.language(), row.text(), importedBy, grammar);
            return Outcome.APPLIED;
        } catch (DomainException refused) {
            rejected.add(
                new ImportReport.RejectedRow(
                    row.rowNumber(),
                    row.key(),
                    row.language(),
                    refused.error().key(),
                    refused.params()
                )
            );
            return Outcome.REJECTED;
        }
    }

    @Nullable private static String textOf(TranslationKey key, String language) {
        TranslationKey.LanguageOverride override = key.overrides().get(language);
        return override == null ? null : override.text();
    }

    private List<ImportReport.MissingTranslation> missingAfterImport(Set<String> languages) {
        List<ImportReport.MissingTranslation> missing = new ArrayList<>();
        Set<String> inOrder = new TreeSet<>(languages);

        for (TranslationKey key : translations.findAll()) {
            for (String language : inOrder) {
                if (key.resolve(language).filter(text -> !text.isBlank()).isEmpty()) {
                    missing.add(new ImportReport.MissingTranslation(key.key(), language));
                }
            }
        }
        return missing;
    }

    private enum Outcome {
        APPLIED,
        CLEARED,
        UNCHANGED,
        REJECTED
    }
}
