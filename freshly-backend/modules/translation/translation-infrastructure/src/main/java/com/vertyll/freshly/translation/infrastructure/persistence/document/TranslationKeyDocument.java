package com.vertyll.freshly.translation.infrastructure.persistence.document;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "translation_key")
public record TranslationKeyDocument(
    @Id String key,
    @Field("context") @Indexed String context,
    @Field("declared") @Indexed boolean declared,
    @Field("defaults") Map<String, String> defaults,
    @Field("overrides") Map<String, OverrideValue> overrides,
    @Version @Field("version") @Nullable Long version
) {

    public record OverrideValue(String text, @Nullable String sourceDefault, String author, Instant at) {
    }
}
