package com.vertyll.freshly.auth.api;

import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.common.annotation.RefreshTokenCookie;
import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.security.config.CookieProperties;
import com.vertyll.freshly.security.config.JwtProperties;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token not found in cookies";
    private static final String EMPTY_COOKIE_VALUE = "";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";

    private static final String AUTH_REGISTERED_MSG_KEY = "success.auth.registered";
    private static final String AUTH_EMAIL_VERIFIED_MSG_KEY = "success.auth.emailVerified";
    private static final String AUTH_LOGIN_SUCCESSFUL_MSG_KEY = "success.auth.loginSuccessful";
    private static final String AUTH_TOKEN_REFRESHED_MSG_KEY = "success.auth.tokenRefreshed";
    private static final String AUTH_LOGOUT_SUCCESSFUL_MSG_KEY = "success.auth.logoutSuccessful";
    private static final String AUTH_RESET_EMAIL_SENT_MSG_KEY = "success.auth.resetEmailSent";
    private static final String AUTH_PASSWORD_RESET_MSG_KEY = "success.auth.passwordReset";
    private static final String AUTH_PASSWORD_CHANGED_MSG_KEY = "success.auth.passwordChanged";
    private static final String AUTH_EMAIL_CHANGE_INITIATED_MSG_KEY =
            "success.auth.emailChangeInitiated";

    private static final long COOKIE_MAX_AGE_ZERO = 0L;
    private static final long MILLISECONDS_TO_SECONDS_DIVISOR = 1000L;

    private final AuthService authService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;
    private final MessageSource messageSource;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterUserRequestDto request) {

        log.info("Registering new user: {}", request.username());

        UUID userId = authService.registerUser(request);

        AuthResponseDto response = new AuthResponseDto(userId);

        return ApiResponse.buildResponse(
                response, AUTH_REGISTERED_MSG_KEY, messageSource, HttpStatus.CREATED);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        log.info("Verifying email with token");

        authService.verifyEmail(token);

        return ApiResponse.buildResponse(
                null, AUTH_EMAIL_VERIFIED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request, HttpServletResponse response) {
        log.info("User login attempt: {}", request.username());

        TokenResponseDto tokens = authService.login(request);

        setRefreshTokenCookie(response, tokens.refreshToken());

        AccessTokenResponseDto accessTokenResponse =
                new AccessTokenResponseDto(
                        tokens.accessToken(), tokens.expiresIn(), tokens.tokenType());

        return ApiResponse.buildResponse(
                accessTokenResponse, AUTH_LOGIN_SUCCESSFUL_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refreshToken(
            @RefreshTokenCookie String refreshToken, HttpServletResponse response) {
        log.info("Refreshing access token");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(REFRESH_TOKEN_NOT_FOUND);
        }

        TokenResponseDto tokens = authService.refreshToken(refreshToken);

        setRefreshTokenCookie(response, tokens.refreshToken());

        AccessTokenResponseDto accessTokenResponse =
                new AccessTokenResponseDto(
                        tokens.accessToken(), tokens.expiresIn(), tokens.tokenType());

        return ApiResponse.buildResponse(
                accessTokenResponse, AUTH_TOKEN_REFRESHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RefreshTokenCookie String refreshToken, HttpServletResponse response) {
        log.info("User logout");

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        clearRefreshTokenCookie(response);

        return ApiResponse.buildResponse(
                null, AUTH_LOGOUT_SUCCESSFUL_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        log.info("Password reset requested for email: {}", request.email());

        authService.initiatePasswordReset(request);

        return ApiResponse.buildResponse(
                null, AUTH_RESET_EMAIL_SENT_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        log.info("Resetting password with token");

        authService.resetPassword(request);

        return ApiResponse.buildResponse(
                null, AUTH_PASSWORD_RESET_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Changing password for user: {}", userId);

        authService.changePassword(userId, request);

        return ApiResponse.buildResponse(
                null, AUTH_PASSWORD_CHANGED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PutMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @Valid @RequestBody ChangeEmailRequestDto request, @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Changing email for user: {}", userId);

        authService.changeEmail(userId, request);

        return ApiResponse.buildResponse(
                null, AUTH_EMAIL_CHANGE_INITIATED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie =
                ResponseCookie.from(jwtProperties.refreshToken().cookieName(), refreshToken)
                        .httpOnly(cookieProperties.httpOnly())
                        .secure(cookieProperties.secure())
                        .path(cookieProperties.path())
                        .maxAge(
                                jwtProperties.refreshToken().expiration()
                                        / MILLISECONDS_TO_SECONDS_DIVISOR)
                        .sameSite(cookieProperties.sameSite())
                        .build();

        response.addHeader(SET_COOKIE_HEADER, cookie.toString());

        log.debug("Refresh token cookie set");
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie =
                ResponseCookie.from(jwtProperties.refreshToken().cookieName(), EMPTY_COOKIE_VALUE)
                        .httpOnly(cookieProperties.httpOnly())
                        .secure(cookieProperties.secure())
                        .path(cookieProperties.path())
                        .maxAge(COOKIE_MAX_AGE_ZERO)
                        .sameSite(cookieProperties.sameSite())
                        .build();

        response.addHeader(SET_COOKIE_HEADER, cookie.toString());

        log.debug("Refresh token cookie cleared");
    }
}
