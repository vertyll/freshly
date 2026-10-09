# Token audience

Which access tokens the API accepts.

Besides the signature, the issuer and the expiry, the resource server requires `freshly-api` in
the token's `aud` claim (`spring.security.oauth2.resourceserver.jwt.audiences`). The realm adds
it to tokens issued to `freshly-app-client`; a token Keycloak issued to any other client of the
realm is refused.
