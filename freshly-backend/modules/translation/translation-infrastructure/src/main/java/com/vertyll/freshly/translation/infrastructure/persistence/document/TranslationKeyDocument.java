package com.vertyll.freshly.translation.infrastructure.persistence.document;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "translation_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationKeyDocument {
    @Id
    @Field("key")
    private String key;

    @Field("context")
    @Indexed
    private String context;

    @Field("declared")
    @Indexed
    private boolean declared;

    @Field("defaults")
    private Map<String, String> defaults;

    @Field("overrides")
    private Map<String, OverrideValue> overrides;

    @Version
    @Field("version")
    @Nullable private Long version;

    public record OverrideValue(String text, @Nullable String sourceDefault, String author, Instant at) {
    }
}
