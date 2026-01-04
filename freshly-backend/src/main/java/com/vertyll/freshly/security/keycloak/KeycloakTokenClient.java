package com.vertyll.freshly.security.keycloak;

import com.vertyll.freshly.auth.api.dto.TokenResponseDto;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    private String getTokenUrl() {
        return properties.serverUrl() + "/realms/" + properties.realm()
                + "/protocol/openid-connect/token";
    }

    private String getLogoutUrl() {
        return properties.serverUrl() + "/realms/" + properties.realm()
                + "/protocol/openid-connect/logout";
    }

    /**
     * Get tokens using username/password (Resource Owner Password Credentials Grant)
     */
    public TokenResponseDto getToken(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, "password");
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add("username", username);
        formData.add("password", password);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return mapToTokenResponse(response);

        } catch (HttpClientErrorException.Unauthorized _) {
            log.warn("Login failed for user: {}", username);
            throw new InvalidPasswordException("Invalid username or password");
        } catch (RestClientException e) {
            log.error("Error during login for user: {}", username, e);
            throw new KeycloakClientException("Login failed", e);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public TokenResponseDto refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, REFRESH_TOKEN);
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add(REFRESH_TOKEN, refreshToken);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return mapToTokenResponse(response);

        } catch (HttpClientErrorException.Unauthorized _) {
            log.warn("Refresh token invalid or expired");
            throw new InvalidPasswordException("Refresh token invalid or expired");
        } catch (RestClientException e) {
            log.error("Error during token refresh", e);
            throw new KeycloakClientException("Token refresh failed", e);
        }
    }

    /**
     * Logout (revoke refresh token)
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Logout must always succeed
    public void logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add(REFRESH_TOKEN, refreshToken);

        try {
            restClient.post()
                    .uri(getLogoutUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("User logged out successfully");

        } catch (Exception e) {
            log.error("Error during logout", e);
            // Don't throw - logout should always succeed from user perspective
        }
    }

    private TokenResponseDto mapToTokenResponse(Map<String, Object> response) {
        if (response == null) {
            throw new KeycloakClientException("Empty response from Keycloak");
        }

        return new TokenResponseDto(
                (String) response.get("access_token"),
                (String) response.get(REFRESH_TOKEN),
                (Integer) response.get("expires_in"),
                (Integer) response.get("refresh_expires_in"),
                (String) response.getOrDefault("token_type", "Bearer")
        );
    }
}
