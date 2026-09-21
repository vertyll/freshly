package com.vertyll.freshly.translation.infrastructure.web.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.domain.error.TranslationError;

final class JwtAuthor {
    private static final String AUTHOR_CLAIM = "preferred_username";

    private JwtAuthor() {
    }

    static String of(Jwt jwt) {
        String username = jwt.getClaimAsString(AUTHOR_CLAIM);
        if (username == null) {
            throw new DomainException(TranslationError.UNATTRIBUTABLE_AUTHOR);
        }
        return username;
    }
}
