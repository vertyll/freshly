package com.vertyll.freshly.auth.api;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.security.config.CookieProperties;
import com.vertyll.freshly.security.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterUserRequestDto request
    ) {

        log.info("Registering new user: {}", request.username());

        UUID userId = authService.registerUser(
                request.username(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        AuthResponseDto response = new AuthResponseDto(
                userId,
                "Registration successful. Please check your email to verify your account."
        );

        return ApiResponse.buildResponse(response, "User registered successfully", HttpStatus.CREATED);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        log.info("Verifying email with token");

        authService.verifyEmail(token);

        return ApiResponse.buildResponse(
                null,
                "Email verified successfully. You can now log in.",
                HttpStatus.OK
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response
    ) {
        log.info("User login attempt: {}", request.username());

        TokenResponseDto tokens = authService.login(request.username(), request.password());

        setRefreshTokenCookie(response, tokens.refreshToken());

        AccessTokenResponseDto accessTokenResponse = new AccessTokenResponseDto(
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.tokenType()
        );

        return ApiResponse.buildResponse(accessTokenResponse, "Login successful", HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("Refreshing access token");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token not found in cookies");
        }

        TokenResponseDto tokens = authService.refreshToken(refreshToken);

        setRefreshTokenCookie(response, tokens.refreshToken());

        AccessTokenResponseDto accessTokenResponse = new AccessTokenResponseDto(
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.tokenType()
        );

        return ApiResponse.buildResponse(accessTokenResponse, "Token refreshed successfully", HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("User logout");

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        clearRefreshTokenCookie(response);

        return ApiResponse.buildResponse(null, "Logout successful", HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request
    ) {
        log.info("Password reset requested for email: {}", request.email());

        authService.initiatePasswordReset(request.email());

        return ApiResponse.buildResponse(
                null,
                "If an account with this email exists, you will receive a password reset link.",
                HttpStatus.OK
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request
    ) {
        log.info("Resetting password with token");

        authService.resetPassword(request.token(), request.newPassword());

        return ApiResponse.buildResponse(
                null,
                "Password reset successful. You can now log in with your new password.",
                HttpStatus.OK
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Changing password for user: {}", userId);

        authService.changePassword(userId, request.currentPassword(), request.newPassword());

        return ApiResponse.buildResponse(
                null,
                "Password changed successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @Valid @RequestBody ChangeEmailRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Changing email for user: {}", userId);

        authService.changeEmail(userId, request.newEmail());

        return ApiResponse.buildResponse(
                null,
                "Email change initiated. Please check your new email to verify.",
                HttpStatus.OK
        );
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(
                        jwtProperties.refreshToken().cookieName(),
                        refreshToken
                )
                .httpOnly(cookieProperties.httpOnly())
                .secure(cookieProperties.secure())
                .path(cookieProperties.path())
                .maxAge(jwtProperties.refreshToken().expiration() / 1000)
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Refresh token cookie set");
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(
                        jwtProperties.refreshToken().cookieName(),
                        ""
                )
                .httpOnly(cookieProperties.httpOnly())
                .secure(cookieProperties.secure())
                .path(cookieProperties.path())
                .maxAge(0)
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Refresh token cookie cleared");
    }
}
