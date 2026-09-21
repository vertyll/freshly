package com.vertyll.freshly.auth.application.port.outbound;

import com.vertyll.freshly.auth.application.dto.AuthTokens;

public interface TokenIssuerPort {
    AuthTokens issue(String username, String password);

    AuthTokens refresh(String refreshToken);

    void revoke(String refreshToken);
}
