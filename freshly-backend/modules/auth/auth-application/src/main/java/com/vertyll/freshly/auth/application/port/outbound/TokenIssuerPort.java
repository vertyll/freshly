package com.vertyll.freshly.auth.application.port.outbound;

import com.vertyll.freshly.auth.domain.model.AuthSession;

public interface TokenIssuerPort {
    AuthSession exchange(String code, String codeVerifier);

    AuthSession refresh(String refreshToken);

    void revoke(String refreshToken);
}
