package com.vertyll.freshly.auth.application.port.outbound;

public interface SessionRevocationPort {
    void revoke(String refreshToken);
}
