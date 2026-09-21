package com.vertyll.freshly.auth.application.port.outbound;

import java.util.UUID;

import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.VerificationToken;

public interface VerificationTokenPort {
    String issue(UUID userId, String email, TokenPurpose purpose);

    VerificationToken validate(String token, TokenPurpose expectedPurpose);
}
