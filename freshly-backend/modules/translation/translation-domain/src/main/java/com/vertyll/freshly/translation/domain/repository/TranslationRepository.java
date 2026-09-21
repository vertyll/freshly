package com.vertyll.freshly.translation.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.translation.domain.model.TranslationKey;

public interface TranslationRepository {
    TranslationKey save(TranslationKey translationKey);

    List<TranslationKey> saveAll(Collection<TranslationKey> translationKeys);

    Optional<TranslationKey> findByKey(String key);

    Map<String, TranslationKey> findAllByKeys(Collection<String> keys);

    List<TranslationKey> findByContext(String context);

    List<TranslationKey> findAll();

    List<TranslationKey> findOrphaned();

    void deleteByKey(String key);

    PageResult<TranslationKey> findAll(PageRequest pageRequest);

    PageResult<TranslationKey> search(String fragment, PageRequest pageRequest);
}
