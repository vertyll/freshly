package com.vertyll.freshly.auth.infrastructure.web.session;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import com.vertyll.freshly.auth.infrastructure.keycloak.KeycloakConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class HostedSignInRequests implements OAuth2AuthorizationRequestResolver {
    public static final String AUTHORIZE_PATH = "/auth/authorize";

    private static final String KC_ACTION = "kc_action";
    private static final Set<String> ALLOWED_ACTIONS = Set.of("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential");
    private static final Set<String> UI_LOCALES = Set.of("pl", "en");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public HostedSignInRequests(ClientRegistrationRepository registrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
            registrations,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        );
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    }

    @Override
    public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + AUTHORIZE_PATH)
                ? resolve(request, KeycloakConfig.REGISTRATION_ID) : null;
    }

    @Override
    @SuppressFBWarnings(
        value = "SERVLET_PARAMETER",
        justification = "kc_action must be on the allowlist and register is only read as a boolean"
    )
    public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorization = delegate.resolve(request, clientRegistrationId);
        if (authorization == null) {
            return null;
        }
        String language = request.getLocale().getLanguage();
        String action = request.getParameter(KC_ACTION);
        boolean register = Boolean.parseBoolean(request.getParameter("register"));
        return OAuth2AuthorizationRequest.from(authorization).additionalParameters(parameters -> {
            if (UI_LOCALES.contains(language)) {
                parameters.put("ui_locales", language);
            }
            if (action != null && ALLOWED_ACTIONS.contains(action)) {
                parameters.put(KC_ACTION, action);
            }
            if (register) {
                parameters.put("prompt", "create");
            }
        }).build();
    }
}
