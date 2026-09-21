package com.vertyll.freshly.translation.application.service.query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.translation.application.dto.TranslationBundle;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;
import com.vertyll.freshly.translation.domain.model.SupportedLanguage;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationQueryService implements TranslationQueryUseCase {
    private static final String DIGEST = "SHA-256";
    private static final int ETAG_LENGTH = 16;

    private final TranslationRepository translations;

    public TranslationQueryService(TranslationRepository translations) {
        this.translations = translations;
    }

    @Override
    public TranslationBundle bundleFor(String language) {
        SupportedLanguage supported = SupportedLanguage.require(language);

        Map<String, String> messages = new TreeMap<>();
        for (TranslationKey key : translations.findAll()) {
            key.resolve(supported.tag()).ifPresent(text -> messages.put(key.key(), text));
        }

        return new TranslationBundle(supported.tag(), messages, etagOf(messages));
    }

    @Override
    public Optional<String> resolve(String key, String language) {
        SupportedLanguage supported = SupportedLanguage.require(language);
        return translations.findByKey(key).flatMap(found -> found.resolve(supported.tag()));
    }

    @Override
    public PagedResponse<TranslationEntry> list(PageRequest pageRequest) {
        return PagedResponse.from(translations.findAll(pageRequest)).map(this::toEntry);
    }

    @Override
    public PagedResponse<TranslationEntry> search(String fragment, PageRequest pageRequest) {
        return PagedResponse.from(translations.search(fragment, pageRequest)).map(this::toEntry);
    }

    @Override
    public List<TranslationEntry> staleOverrides() {
        return translations.findAll()
            .stream()
            .filter(key -> !key.staleOverrides().isEmpty())
            .map(this::toEntry)
            .toList();
    }

    @Override
    public List<TranslationEntry> orphans() {
        return translations.findOrphaned().stream().map(this::toEntry).toList();
    }

    @Override
    public List<String> supportedLanguages() {
        return SupportedLanguage.tags();
    }

    private TranslationEntry toEntry(TranslationKey key) {
        return TranslationEntry.from(key, SupportedLanguage.tags());
    }

    private static String etagOf(Map<String, String> messages) {
        StringBuilder joined = new StringBuilder();
        messages.forEach((key, value) -> joined.append(key).append('\u001f').append(value).append('\u001e'));

        try {
            byte[] digest =
                    MessageDigest.getInstance(DIGEST).digest(joined.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, ETAG_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
