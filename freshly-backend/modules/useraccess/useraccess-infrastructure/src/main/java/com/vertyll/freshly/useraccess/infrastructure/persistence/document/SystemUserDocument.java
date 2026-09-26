package com.vertyll.freshly.useraccess.infrastructure.persistence.document;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "useraccess_system_user")
public record SystemUserDocument(
    @Id UUID keycloakUserId,
    @Field("is_active") @Indexed boolean active,
    @Field("roles") Set<String> roles,
    @Version @Field("version") @Nullable Long version
) {
}
