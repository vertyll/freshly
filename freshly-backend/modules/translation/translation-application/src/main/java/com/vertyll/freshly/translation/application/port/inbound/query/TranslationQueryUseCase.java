package com.vertyll.freshly.translation.application.port.inbound.query;

import java.util.List;
import java.util.Optional;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.translation.application.dto.TranslationBundle;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;

public interface TranslationQueryUseCase {
    TranslationBundle bundleFor(String language);

    Optional<String> resolve(String key, String language);

    PagedResponse<TranslationEntry> list(PageRequest pageRequest);

    PagedResponse<TranslationEntry> search(String fragment, PageRequest pageRequest);

    List<TranslationEntry> staleOverrides();

    List<TranslationEntry> orphans();

    List<String> supportedLanguages();
}
