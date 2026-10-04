# auth

Browser sessions on top of Keycloak's hosted pages. Registration, sign-in, e-mail
verification, password reset, two-factor setup and acceptance of the terms all happen on
Keycloak's own pages; this module starts that journey, finishes it, and turns the result
into a session the rest of the application can use. The OAuth2 work itself — state, PKCE,
the code exchange, the ID token check and the refresh — is Spring Security's OAuth2 client.

## The flow

```
GET  /auth/authorize   ->  302 to Keycloak with state, PKCE challenge, ui_locales
                           (?register=true adds prompt=create, ?kc_action=... a whitelisted action)
GET  /auth/callback    <-  Keycloak returns a code; Spring checks the state against this browser,
                           exchanges the code and checks the ID token; the account is provisioned
GET  /auth/session         who is signed in, or 204
POST /auth/logout          revokes the refresh token at Keycloak and ends the session
```

After the callback the browser carries one cookie, `FRESHLY_SESSION` (`HttpOnly`, `Secure`,
`SameSite=Lax`). The tokens stay on the server, in the session's `OAuth2AuthorizedClient`.
`SessionAccessTokenFilter` runs inside the security chain: for a request whose session holds a
sign-in it asks the `OAuth2AuthorizedClientManager` for the access token, refreshing it when it
is about to expire, decodes it with the resource server's decoder and authenticates the request
with it. Every controller therefore sees the same thing — a Keycloak JWT checked by the resource
server — whether the caller is a browser with a session or a client with a bearer token.

## The shape

| Layer          | What lives there                                                                                                                 |
|----------------|----------------------------------------------------------------------------------------------------------------------------------|
| domain         | `SignedInUser` (who and which roles)                                                                                             |
| application    | `SessionUseCase` and `SessionService`; ports `UserProvisioningPort`, `SessionRevocationPort`                                     |
| infrastructure | `BrowserSignIn` (the OAuth2 client added to the platform's chain), `SignInCompletion`, `SessionAccessTokenFilter`, Keycloak, ACL |

`useraccess` is the only neighbouring module, reached through `infrastructure/acl`. The filter
chain belongs to `platform/shared-security`; `BrowserSignIn` is a `SecurityChainCustomizer`
that adds the sign-in to it instead of building a second chain.

## Decisions worth knowing

### Keycloak owns every page that touches a credential

The module never sees a password. Keycloak's pages carry its brute-force protection,
password policy, e-mail verification, terms of use and two-factor setup, which a password
grant would bypass or make the application re-implement. The realm allows no password
grant for `freshly-app-client`, only the authorization code flow with PKCE.

With e-mail verification on, Keycloak 26.7 asks for no password at registration: the account
is created, the terms are accepted, the verification link is sent, and the password is set
after the link is opened.

### State and PKCE are tied to the browser that started

`HostedSignInRequests` builds the authorization request with Spring's resolver and PKCE, and
adds `ui_locales`, `prompt=create` and the allowed `kc_action`. Spring keeps the request in the
server-side session and the callback takes it out once: a callback whose state was not issued
to this browser is refused (`?error=state_mismatch`), so a code obtained elsewhere cannot be
planted in someone's session. Spring changes the session identifier at sign-in, so one known
before sign-in is worthless after it.

### Provisioning happens at sign-in, and undoes the session if it fails

`SignInCompletion` runs after Spring has the tokens: it reads the person from the access token
and `SessionService.signIn` creates the `useraccess` record when there is none. If the token is
unusable or the record cannot be written, the refresh token Keycloak just issued is revoked and
the session is ended, so a person is never signed in without the account the rest of the
application expects.

Roles come from the access token, minus Keycloak's built-ins (`default-roles-*`,
`offline_access`, `uma_authorization`).

### Cross-site writes get no token

The session cookie is `SameSite=Lax`, which already keeps it off cross-site `POST`, `PUT`,
`PATCH` and `DELETE`. Lax still treats another subdomain of the same site as same-site, so
`SessionAccessTokenFilter` also reads `Sec-Fetch-Site`: an unsafe method sent from anywhere other
than this origin is treated as anonymous and answered 401. `POST /auth/logout` applies the same
rule and answers 403, so another page cannot sign the person out.

### One refresh per refresh token

Keycloak rotates refresh tokens and refuses a reused one, so two requests refreshing the same
session at once would sign the person out. `SingleFlightRefreshTokenProvider` wraps Spring's
refresh provider and runs a single refresh per refresh token: a second request with the same
token waits for the first and receives its result, and for thirty seconds afterwards a request
still holding the old token receives the same result instead of reaching Keycloak. A refresh
Keycloak refuses ends the session and is not remembered; one that fails because Keycloak is
unreachable leaves the session for the next request.

Replicas agree through Redis (`SharedRefreshes`). After the in-process single flight, a replica
claims `freshly:refresh-lock:<sha256 of the refresh token>` for ten seconds, calls Keycloak and
leaves the new tokens under `freshly:refresh-result:<sha256>` for thirty seconds; a replica that
finds the lock taken waits for that result instead of presenting the token again. A refused
refresh releases the lock and is not shared. When Redis is unreachable a replica refreshes on its
own, so Redis never becomes a reason a request fails.

### Sessions live in Redis

Spring Session keeps them under the `freshly:session` key namespace, so the application holds
no state of its own: a restart signs nobody out and replicas can be added.

### The API accepts only its own tokens

Besides the signature, the issuer and the expiry, the resource server requires `freshly-api` in
the token's `aud` claim (`spring.security.oauth2.resourceserver.jwt.audiences`). The realm adds
it to tokens issued to `freshly-app-client`; a token Keycloak issued to any other client of the
realm is refused.

## Testing

| Tier        | Where                                                                                                                                                                                   |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Domain      | `SignedInUserTest`: the roles are copied                                                                                                                                                |
| Application | `SessionServiceTest`: provisioning, revocation when provisioning fails                                                                                                                  |
| Integration | `bootstrap/.../SharedRefreshesTest`: one refresh across replicas, on a Redis container                                                                                                  |
| Integration | `bootstrap/.../HostedSignInTest`: authorize, state check, sign-in with provisioning, refresh, refused refresh, bearer token, cross-site write, logout — on MongoDB and Redis containers |
