package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "system_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserDocument {

    @Id private UUID keycloakUserId;

    private boolean isActive;
    private Set<String> roles;

    @Version @Nullable private Long version;
}
