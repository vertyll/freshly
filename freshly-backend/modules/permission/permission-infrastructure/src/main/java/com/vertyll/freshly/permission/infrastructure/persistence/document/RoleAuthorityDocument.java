package com.vertyll.freshly.permission.infrastructure.persistence.document;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "role_authority")
public record RoleAuthorityDocument(
    @Id String role,
    @Field("unrestricted") boolean unrestricted,
    @Field("permissions") Set<String> permissions,
    @Version @Field("version") @Nullable Long version
) {
}
