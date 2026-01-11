package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.vertyll.freshly.useraccess.domain.UserRoleEnum;

@Document(collection = "system_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUserDocument {

    @Id private UUID keycloakUserId;

    private boolean isActive;
    private Set<UserRoleEnum> roles;
}
