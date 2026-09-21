package com.vertyll.freshly.translation.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;
import com.vertyll.freshly.translation.infrastructure.persistence.document.TranslationKeyDocument;
import com.vertyll.freshly.translation.infrastructure.persistence.repository.SpringDataTranslationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TranslationPersistenceAdapter implements TranslationRepository {

    private static final Sort BY_KEY = Sort.by(Sort.Direction.ASC, "key");

    private final SpringDataTranslationRepository repository;

    @Override
    public TranslationKey save(TranslationKey translationKey) {
        return toDomain(repository.save(toDocument(translationKey)));
    }

    @Override
    public List<TranslationKey> saveAll(Collection<TranslationKey> translationKeys) {
        return repository.saveAll(translationKeys.stream().map(TranslationPersistenceAdapter::toDocument).toList())
            .stream()
            .map(TranslationPersistenceAdapter::toDomain)
            .toList();
    }

    @Override
    public Optional<TranslationKey> findByKey(String key) {
        return repository.findById(key).map(TranslationPersistenceAdapter::toDomain);
    }

    @Override
    public Map<String, TranslationKey> findAllByKeys(Collection<String> keys) {
        Map<String, TranslationKey> found = new LinkedHashMap<>();
        repository.findAllById(keys).forEach(document -> found.put(document.getKey(), toDomain(document)));
        return found;
    }

    @Override
    public List<TranslationKey> findByContext(String context) {
        return repository.findByContext(context).stream().map(TranslationPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<TranslationKey> findAll() {
        return repository.findAll(BY_KEY).stream().map(TranslationPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<TranslationKey> findOrphaned() {
        return repository.findByDeclaredFalseOrderByKeyAsc()
            .stream()
            .map(TranslationPersistenceAdapter::toDomain)
            .toList();
    }

    @Override
    public void deleteByKey(String key) {
        repository.deleteById(key);
    }

    @Override
    public PageResult<TranslationKey> findAll(PageRequest pageRequest) {
        return toPage(repository.findAll(pageable(pageRequest)), pageRequest);
    }

    @Override
    public PageResult<TranslationKey> search(String fragment, PageRequest pageRequest) {
        return toPage(repository.search(Pattern.quote(fragment), pageable(pageRequest)), pageRequest);
    }

    private static org.springframework.data.domain.PageRequest pageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), BY_KEY);
    }

    private static PageResult<TranslationKey> toPage(Page<TranslationKeyDocument> page, PageRequest request) {
        return new PageResult<>(
            page.getContent().stream().map(TranslationPersistenceAdapter::toDomain).toList(),
            request.page(),
            request.size(),
            page.getTotalElements()
        );
    }

    private static TranslationKeyDocument toDocument(TranslationKey key) {
        Map<String, TranslationKeyDocument.OverrideValue> overrides = new LinkedHashMap<>();
        key.overrides()
            .forEach(
                (language, override) -> overrides.put(
                    language,
                    new TranslationKeyDocument.OverrideValue(
                        override.text(),
                        override.sourceDefault(),
                        override.author(),
                        override.at()
                    )
                )
            );

        return new TranslationKeyDocument(
            key.key(),
            key.context(),
            key.declared(),
            key.defaults(),
            overrides,
            key.version()
        );
    }

    private static TranslationKey toDomain(TranslationKeyDocument document) {
        Map<String, TranslationKey.LanguageOverride> overrides = new LinkedHashMap<>();
        document.getOverrides()
            .forEach(
                (language, value) -> overrides.put(
                    language,
                    new TranslationKey.LanguageOverride(value.text(), value.sourceDefault(), value.author(), value.at())
                )
            );

        return TranslationKey.reconstitute(
            document.getKey(),
            document.getContext(),
            document.isDeclared(),
            document.getDefaults(),
            overrides,
            document.getVersion()
        );
    }
}
