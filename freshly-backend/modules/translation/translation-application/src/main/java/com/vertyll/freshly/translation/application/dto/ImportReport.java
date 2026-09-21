package com.vertyll.freshly.translation.application.dto;

import java.util.List;
import java.util.Map;

public record ImportReport(
    int applied,
    int cleared,
    int unchanged,
    List<String> unknownKeys,
    List<String> unknownLanguages,
    List<RejectedRow> rejected,
    List<MissingTranslation> missing
) {
    public record RejectedRow(int rowNumber, String key, String language, String code, Map<String, Object> params) {
        public RejectedRow {
            params = Map.copyOf(params);
        }
    }

    public record MissingTranslation(String key, String language) {
    }

    public ImportReport {
        unknownKeys = List.copyOf(unknownKeys);
        unknownLanguages = List.copyOf(unknownLanguages);
        rejected = List.copyOf(rejected);
        missing = List.copyOf(missing);
    }
}
