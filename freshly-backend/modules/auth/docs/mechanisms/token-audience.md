# Token audience

Which access tokens the API accepts.

Besides the signature, the issuer and the expiry, the resource server requires `freshly-api` in
the token's `aud` claim (`spring.security.oauth2.resourceserver.jwt.audiences`). The realm adds
it to tokens issued to `freshly-app-client`; a token Keycloak issued to any other client of the
realm is refused.

The token must also be an access token. Keycloak signs its ID tokens with the same keys and gives both the header
`typ` `JWT`, as the realm issues no RFC 9068 `at+jwt` header, so the header cannot tell them apart; Keycloak's own
`typ` claim can: `Bearer` on an access token, `ID` on an ID token. `SecurityConfig` adds a validator that refuses any
other value, and Spring Boot runs it beside the issuer, audience and expiry checks.
