package com.vertyll.freshly.permission.infrastructure.persistence.document;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "role_authority")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuthorityDocument {
    @Id
    @Field("role")
    private String role;

    @Field("unrestricted")
    private boolean unrestricted;

    @Field("permissions")
    private Set<String> permissions;

    @Version
    @Field("version")
    @Nullable private Long version;
}
