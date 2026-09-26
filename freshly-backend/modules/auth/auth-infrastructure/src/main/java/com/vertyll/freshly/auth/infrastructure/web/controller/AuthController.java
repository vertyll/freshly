package com.vertyll.freshly.auth.infrastructure.web.controller;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.vertyll.freshly.auth.application.dto.AuthTokens;
import com.vertyll.freshly.auth.application.port.inbound.command.CredentialsUseCase;
import com.vertyll.freshly.auth.application.port.inbound.command.RegistrationUseCase;
import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.infrastructure.web.dto.ChangeEmailRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.ChangePasswordRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.ForgotPasswordRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.LoginRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.RegisterUserRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.ResetPasswordRequestDto;
import com.vertyll.freshly.auth.infrastructure.web.dto.TokenResponseDto;
import com.vertyll.freshly.web.security.PublicEndpoint;
import com.vertyll.freshly.web.security.ScopedToCaller;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String AUTH_PATH = "/auth";
    private static final String SUBJECT_CLAIM = "sub";

    private final RegistrationUseCase registration;
    private final SessionUseCase sessions;
    private final CredentialsUseCase credentials;

    @PostMapping("/register")
    @PublicEndpoint("registration is how an account first comes to exist")
    public ResponseEntity<RegisteredUser> register(@Valid @RequestBody RegisterUserRequestDto request) {
        UUID userId = registration.register(request.toCommand(callerLanguage()));

        return ResponseEntity.created(URI.create("/users/" + userId)).body(new RegisteredUser(userId));
    }

    @GetMapping("/verify-email")
    @PublicEndpoint("the link is the credential; the recipient is not signed in yet")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        registration.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @PublicEndpoint("signing in cannot require being signed in")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthTokens tokens = sessions.login(request.username(), request.password());

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        refreshCookie(tokens).ifPresent(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));

        return response.body(TokenResponseDto.from(tokens));
    }

    @PostMapping("/refresh")
    @PublicEndpoint("the refresh cookie is the credential; the access token has expired")
    public ResponseEntity<TokenResponseDto> refresh(
        @CookieValue(name = REFRESH_COOKIE, required = false) @Nullable String refreshToken
    ) {
        AuthTokens tokens = sessions.refresh(refreshToken);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        refreshCookie(tokens).ifPresent(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));

        return response.body(TokenResponseDto.from(tokens));
    }

    @PostMapping("/logout")
    @PublicEndpoint("logging out must work even with an expired access token")
    public ResponseEntity<Void> logout(
        @CookieValue(name = REFRESH_COOKIE, required = false) @Nullable String refreshToken
    ) {
        sessions.logout(refreshToken);

        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString()).build();
    }

    @PostMapping("/forgot-password")
    @PublicEndpoint("someone who has forgotten their password cannot sign in to ask")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        credentials.initiatePasswordReset(request.email(), callerLanguage());

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @PublicEndpoint("the reset token is the credential")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        credentials.resetPassword(request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @ScopedToCaller("changes only the calling user's own password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        credentials.changePassword(request.toCommand(callerId(jwt)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-email")
    @ScopedToCaller("changes only the calling user's own address")
    public ResponseEntity<Void> changeEmail(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChangeEmailRequestDto request
    ) {
        credentials.changeEmail(request.toCommand(callerId(jwt), callerLanguage()));

        return ResponseEntity.accepted().build();
    }

    private static Optional<ResponseCookie> refreshCookie(AuthTokens tokens) {
        String value = tokens.refreshToken();
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(
            ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(refreshCookiePath())
                .maxAge(Duration.ofSeconds(tokens.refreshExpiresInSeconds()))
                .build()
        );
    }

    private static String refreshCookiePath() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(AUTH_PATH).build().getPath();
    }

    private static ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path(refreshCookiePath())
            .maxAge(Duration.ZERO)
            .build();
    }

    public record RegisteredUser(UUID userId) {
    }

    private static String callerLanguage() {
        return LocaleContextHolder.getLocale().toLanguageTag();
    }

    private static UUID callerId(Jwt jwt) {
        String subject = jwt.getClaimAsString(SUBJECT_CLAIM);
        if (subject == null) {
            throw new AuthenticationCredentialsNotFoundException("Token carries no subject");
        }
        return UUID.fromString(subject);
    }
}
