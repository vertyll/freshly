package com.vertyll.freshly.useraccess.infrastructure.persistence.document;

import java.util.Set;
import java.util.UUID;

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

@Document(collection = "useraccess_system_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserDocument {
    @Id
    private UUID keycloakUserId;

    @Field("is_active")
    @Indexed
    private boolean active;

    @Field("roles")
    private Set<String> roles;

    @Version
    @Field("version")
    @Nullable private Long version;
}
