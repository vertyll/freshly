package com.vertyll.freshly.auth.api;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
}
