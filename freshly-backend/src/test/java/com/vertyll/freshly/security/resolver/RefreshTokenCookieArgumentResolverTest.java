package com.vertyll.freshly.security.resolver;

import com.vertyll.freshly.security.annotation.RefreshTokenCookie;
import com.vertyll.freshly.security.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCookieArgumentResolverTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtProperties.RefreshToken refreshToken;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    private RefreshTokenCookieArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshToken);
        lenient().when(refreshToken.cookieName()).thenReturn("refresh_token");
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
                new Cookie("session_id", "session123"),
                new Cookie("refresh_token", "token123"),
                new Cookie("other", "value")
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo("token123");
    }

    @Test
    @DisplayName("Should return null when refresh token cookie not found")
    void shouldReturnNullWhenRefreshTokenCookieNotFound() {
        // Given
        Cookie[] cookies = {
                new Cookie("session_id", "session123"),
                new Cookie("other", "value")
        };

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
        when(refreshToken.cookieName()).thenReturn("custom_refresh_token");
        resolver = new RefreshTokenCookieArgumentResolver(jwtProperties);

        Cookie[] cookies = {
                new Cookie("refresh_token", "wrong_token"),
                new Cookie("custom_refresh_token", "correct_token")
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo("correct_token");
    }

    @Test
    @DisplayName("Should return first matching cookie when multiple cookies with same name exist")
    void shouldReturnFirstMatchingCookieWhenMultipleCookiesWithSameNameExist() {
        // Given
        Cookie[] cookies = {
                new Cookie("refresh_token", "first_token"),
                new Cookie("refresh_token", "second_token")
        };

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // When
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // Then
        assertThat(result).isEqualTo("first_token");
    }
}
