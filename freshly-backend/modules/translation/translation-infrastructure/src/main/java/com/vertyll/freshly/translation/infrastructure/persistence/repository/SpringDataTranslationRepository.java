package com.vertyll.freshly.translation.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.vertyll.freshly.translation.infrastructure.persistence.document.TranslationKeyDocument;

public interface SpringDataTranslationRepository extends MongoRepository<TranslationKeyDocument, String> {
    List<TranslationKeyDocument> findByContext(String context);

    List<TranslationKeyDocument> findByDeclaredFalseOrderByKeyAsc();

    @Query(
        "{ $or: [ " + "{ 'key':       { $regex: ?0, $options: 'i' } }, "
                + "{ 'context':   { $regex: ?0, $options: 'i' } } " + "] }"
    )
    Page<TranslationKeyDocument> search(String fragment, Pageable pageable);
}
