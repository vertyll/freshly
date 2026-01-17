package com.vertyll.freshly.auth.keycloak;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.auth.api.dto.TokenResponseDto;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;
import com.vertyll.freshly.auth.keycloak.exception.KeycloakClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_TYPE_KEY = "token_type";
    private static final String BEARER = "Bearer";

    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private static final String REFRESH_EXPIRES_IN_KEY = "refresh_expires_in";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

    private static final String EMPTY_RESPONSE = "Empty response from Keycloak";
    private static final String REFRESH_TOKEN_INVALID = "Refresh token invalid or expired";
    private static final String INVALID_USERNAME_PASSWORD = "Invalid username or password";

    private static final String TOKEN_REFRESH_FAILED = "Token refresh failed";
    private static final String LOGIN_FAILED = "Login failed";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    private String getTokenUrl() {
        return properties.serverUrl()
                + "/realms/"
                + properties.realm()
                + "/protocol/openid-connect/token";
    }

    private String getLogoutUrl() {
        return properties.serverUrl()
                + "/realms/"
                + properties.realm()
                + "/protocol/openid-connect/logout";
    }

    /** Refresh access token using refresh token */
    public TokenResponseDto refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, REFRESH_TOKEN);
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add(REFRESH_TOKEN, refreshToken);

        try {
            Map<String, Object> response =
                    restClient
                            .post()
                            .uri(getTokenUrl())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formData)
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new KeycloakClientException(EMPTY_RESPONSE);
            }

            return mapToTokenResponse(response);

        } catch (HttpClientErrorException.Unauthorized _) {
            throw new InvalidPasswordException(REFRESH_TOKEN_INVALID);
        } catch (RestClientException e) {
            throw new KeycloakClientException(TOKEN_REFRESH_FAILED, e);
        }
    }

    /** Get tokens using username/password (Resource Owner Password Credentials Grant) */
    public TokenResponseDto getToken(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, PASSWORD_KEY);
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add(USERNAME_KEY, username);
        formData.add(PASSWORD_KEY, password);

        try {
            Map<String, Object> response =
                    restClient
                            .post()
                            .uri(getTokenUrl())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formData)
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new KeycloakClientException(EMPTY_RESPONSE);
            }

            return mapToTokenResponse(response);

        } catch (HttpClientErrorException.Unauthorized _) {
            throw new InvalidPasswordException(INVALID_USERNAME_PASSWORD);
        } catch (RestClientException e) {
            throw new KeycloakClientException(LOGIN_FAILED, e);
        }
    }

    /** Logout (revoke refresh token) */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Logout must always succeed
    public void logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(CLIENT_ID, properties.userClientId());
        formData.add(REFRESH_TOKEN, refreshToken);

        try {
            restClient
                    .post()
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
        return new TokenResponseDto(
                (String) response.get(ACCESS_TOKEN_KEY),
                (String) response.get(REFRESH_TOKEN),
                getNumber(response, EXPIRES_IN_KEY),
                getNumber(response, REFRESH_EXPIRES_IN_KEY),
                (String) response.getOrDefault(TOKEN_TYPE_KEY, BEARER));
    }

    private Integer getNumber(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException _) {
                return 0;
            }
        }
        return 0;
    }
}
