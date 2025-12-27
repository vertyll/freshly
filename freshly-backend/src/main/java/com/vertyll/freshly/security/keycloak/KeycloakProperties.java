package com.vertyll.freshly.security.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private String adminClientId;
    private String adminUsername;
    private String adminPassword;
    private String userClientId;
}
