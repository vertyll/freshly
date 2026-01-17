package com.vertyll.freshly.security.resolver;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import com.vertyll.freshly.common.annotation.RefreshTokenCookie;
import com.vertyll.freshly.security.config.JwtProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCookieArgumentResolverTest {
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String SESSION_ID = "session_id";
    private static final String SESSION_123 = "session123";
    private static final String TOKEN_123 = "token123";
    private static final String OTHER = "other";
    private static final String VALUE = "value";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private JwtProperties jwtProperties;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private JwtProperties.RefreshToken refreshToken;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MethodParameter methodParameter;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private NativeWebRequest webRequest;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private HttpServletRequest httpServletRequest;

    private RefreshTokenCookieArgumentResolver resolver;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshToken);
        lenient().when(refreshToken.cookieName()).thenReturn(REFRESH_TOKEN);
        resolver = new RefreshTokenCookieArgumentResolver(jwtProperties);
    }

    @Test
    @DisplayName("Should support parameter with RefreshTokenCookie annotation")
    void shouldSupportParameterWithRefreshTokenCookieAnnotation() {
        // Given
        when(methodParameter.hasParameterAnnotation(RefreshTokenCookie.class)).thenReturn(true);

        // When
        boolean supports = resolver.supportsParameter(methodParameter);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("Should not support parameter without RefreshTokenCookie annotation")
    void shouldNotSupportParameterWithoutRefreshTokenCookieAnnotation() {
        // Given
        when(methodParameter.hasParameterAnnotation(RefreshTokenCookie.class)).thenReturn(false);

        // When
        boolean supports = resolver.supportsParameter(methodParameter);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("Should resolve refresh token from cookie")
    void shouldResolveRefreshTokenFromCookie() {
        // Given
        Cookie[] cookies = {
            new Cookie(SESSION_ID, SESSION_123),
            new Cookie(REFRESH_TOKEN, TOKEN_123),
            new Cookie(OTHER, VALUE)
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo(TOKEN_123);
    }

    @Test
    @DisplayName("Should return null when refresh token cookie not found")
    void shouldReturnNullWhenRefreshTokenCookieNotFound() {
        // Given
        Cookie[] cookies = {new Cookie(SESSION_ID, SESSION_123), new Cookie(OTHER, VALUE)};

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when request has no cookies")
    void shouldReturnNullWhenRequestHasNoCookies() {
        // Given
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(null);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when request is null")
    void shouldReturnNullWhenRequestIsNull() {
        // Given
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when cookies array is empty")
    void shouldReturnNullWhenCookiesArrayIsEmpty() {
        // Given
        Cookie[] cookies = {};

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should use correct cookie name from properties")
    void shouldUseCorrectCookieNameFromProperties() {
        // Given
        String customCookieName = "custom_refresh_token";
        String correctToken = "correct_token";
        when(refreshToken.cookieName()).thenReturn(customCookieName);
        resolver = new RefreshTokenCookieArgumentResolver(jwtProperties);

        Cookie[] cookies = {
            new Cookie(REFRESH_TOKEN, "wrong_token"), new Cookie(customCookieName, correctToken)
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo(correctToken);
    }

    @Test
    @DisplayName("Should return first matching cookie when multiple cookies with same name exist")
    void shouldReturnFirstMatchingCookieWhenMultipleCookiesWithSameNameExist() {
        // Given
        String firstToken = "first_token";
        Cookie[] cookies = {
            new Cookie(REFRESH_TOKEN, firstToken), new Cookie(REFRESH_TOKEN, "second_token")
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo(firstToken);
    }
}
