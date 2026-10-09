# Standards

The specifications this repository implements or depends on, what for, and where the documentation covers it.

| Standard | Title | Used for | Explained in |
|---|---|---|---|
| [RFC 6749](https://www.rfc-editor.org/rfc/rfc6749) | OAuth 2.0 | the authorization code flow and refresh tokens | [auth: The flow](freshly-backend/modules/auth/README.md#the-flow) |
| [RFC 7636](https://www.rfc-editor.org/rfc/rfc7636) | PKCE | binding the code to the browser | [auth: State and PKCE are tied to the browser that started](freshly-backend/modules/auth/README.md#state-and-pkce-are-tied-to-the-browser-that-started) |
| [OIDC Core](https://openid.net/specs/openid-connect-core-1_0.html) | OpenID Connect Core 1.0 | the ID token and the `openid` scope | [auth: The flow](freshly-backend/modules/auth/README.md#the-flow) |
| [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519) | JSON Web Token (JWT) | the access token and its audience | [auth: The API accepts only its own tokens](freshly-backend/modules/auth/README.md#the-api-accepts-only-its-own-tokens) |
| [RFC 6750](https://www.rfc-editor.org/rfc/rfc6750) | OAuth 2.0 Bearer Token Usage | `Authorization: Bearer` from clients with their own token | [auth: The API accepts only its own tokens](freshly-backend/modules/auth/README.md#the-api-accepts-only-its-own-tokens) |
| [RFC 6265bis](https://datatracker.ietf.org/doc/draft-ietf-httpbis-rfc6265bis/) | Cookies: HTTP State Management Mechanism (draft) | the `SameSite=Lax` session cookie | [auth: Cross-site writes get no token](freshly-backend/modules/auth/README.md#cross-site-writes-get-no-token) |
| [Fetch Metadata](https://www.w3.org/TR/fetch-metadata/) | Fetch Metadata Request Headers (W3C) | refusing cross-site writes by `Sec-Fetch-Site` | [auth: Cross-site writes get no token](freshly-backend/modules/auth/README.md#cross-site-writes-get-no-token) |
| [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) | Problem Details for HTTP APIs | the error body of every refusal | [Translations: Field validation carries keys too](freshly-backend/docs/translations.md#field-validation-carries-keys-too) |
| [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110) | HTTP Semantics | `ETag` and `If-Match` on edits, and `Accept-Language` | [Hexagonal Layering: Optimistic locking → VersionGuard](freshly-backend/docs/hexagonal-layering.md#optimistic-locking--versionguard) |
| [ICU MessageFormat](https://unicode-org.github.io/icu/userguide/format_parse/messages/) | ICU MessageFormat | the syntax of every translated message | [Translations: The message format is ICU](freshly-backend/docs/translations.md#the-message-format-is-icu) |
| [OpenAPI](https://spec.openapis.org/oas/latest.html) | OpenAPI Specification | the Swagger UI | [Freshly — backend: API documentation](freshly-backend/README.md#api-documentation) |
| [Unicode CLDR](https://cldr.unicode.org/index/cldr-spec/plural-rules) | Unicode CLDR plural rules | Polish plural forms | [Translations: The message format is ICU](freshly-backend/docs/translations.md#the-message-format-is-icu) |
