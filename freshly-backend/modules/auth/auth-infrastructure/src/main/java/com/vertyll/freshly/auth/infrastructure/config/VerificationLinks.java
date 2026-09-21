package com.vertyll.freshly.auth.infrastructure.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerificationLinks implements VerificationLinkFactory {
    private static final String VERIFY_EMAIL_PATH = "/verify-email?token=";
    private static final String RESET_PASSWORD_PATH = "/reset-password?token=";

    private final FrontendProperties frontend;

    @Override
    public String emailVerificationLink(String token) {
        return base() + VERIFY_EMAIL_PATH + encode(token);
    }

    @Override
    public String passwordResetLink(String token) {
        return base() + RESET_PASSWORD_PATH + encode(token);
    }

    private String base() {
        String url = frontend.url();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
