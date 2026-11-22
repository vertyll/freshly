# Cross-site requests

Why a forged request from another site cannot act with the user's session.

A browser sends the session cookie with every request to this host, whichever page made it, so a write that the
session authenticates also needs a CSRF token, which a page of another site cannot know. Two older defenses stay in
place behind it.

## The CSRF token

Spring Security's CSRF protection is on, with the token kept in a cookie (`CookieCsrfTokenRepository`) and read the way
a single-page application reads it (`csrf.spa()`):

- every response carries the `XSRF-TOKEN` cookie, `Path=/api/v1`, `SameSite=Lax`, and `Secure` exactly when the session
  cookie is; it is not `HttpOnly`, since a script of this origin must read it, and it holds no credential;
- a `POST`, `PUT`, `PATCH` or `DELETE` sends the cookie's value back in the `X-XSRF-TOKEN` header; without it, or with
  a value that does not match, the request is refused with `403` and the `error.security.accessDenied` problem
  document before it reaches a controller;
- the token is replaced at sign-in, so a token known before it is worthless after it.

A page of another site can make the browser send the cookie, but cannot read it, so it cannot write the header. Swagger
UI, the one page this back end serves, sends the header itself (`springdoc.swagger-ui.csrf.enabled`).

## Requests with a bearer token need none

A client that calls the API with its own access token, `Authorization: Bearer`, has no session to forge, and the token
is not asked of it. `BearerTokenRequests` finds the token as the resource server does (`DefaultBearerTokenResolver`), so
exactly the requests the token authenticates are exempt: a request with another scheme, such as `Basic`, is still
authenticated by its session cookie and still needs the CSRF token, and a malformed bearer token is exempt only to be
refused with `401`. A browser never adds an `Authorization` header by itself, and another site's page cannot set one
without a CORS preflight, which `CorsConfig` allows only for the origins of `application.cors.allowed-origins`.

## Defense in depth

- The session cookie is `SameSite=Lax`, which keeps it off cross-site `POST`, `PUT`, `PATCH` and `DELETE`.
- Lax still treats another subdomain of the same site as same-site, and such a subdomain could also set an
  `XSRF-TOKEN` cookie of its own for this host and so know its value. `SessionAccessTokenFilter` therefore also reads
  `Sec-Fetch-Site`: an unsafe method sent from anywhere other than this origin is treated as anonymous and answered
  `401`, whatever token it carries. `POST /auth/logout` applies the same rule and answers `403`, so another page cannot
  sign the person out.
