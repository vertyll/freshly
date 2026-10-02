# auth

Browser sessions on top of Keycloak's hosted pages. Registration, sign-in, e-mail
verification, password reset, two-factor setup and acceptance of the terms all happen on
Keycloak's own pages; this module starts that journey, finishes it, and turns the result
into a session the rest of the application can use.

## The flow

```
GET  /auth/authorize   ->  302 to Keycloak with state, PKCE challenge, ui_locales
                           (?register=true adds prompt=create, ?kc_action=... a whitelisted action)
GET  /auth/callback    <-  Keycloak returns a code; the state is checked against this browser,
                           the code is exchanged, the account is provisioned, the session is set
GET  /auth/session         who is signed in, or 204
POST /auth/logout          revokes the refresh token at Keycloak and ends the session
```

After the callback the browser carries one cookie, `FRESHLY_SESSION` (`HttpOnly`, `Secure`,
`SameSite=Lax`). The tokens stay on the server. `SessionTokenRelayFilter` runs in front of
Spring Security: for a request that has a session and no `Authorization` header it puts the
session's access token in that header, refreshing it first when it is about to expire. Every
controller therefore sees the same thing it saw before — a Keycloak JWT checked by the
resource server — whether the caller is a browser with a session or a client with a bearer
token.

## The shape

| Layer          | What lives there                                                                                                      |
|----------------|-----------------------------------------------------------------------------------------------------------------------|
| domain         | `AuthSession` (who, which roles, the tokens, when the access token expires), `AuthError`                              |
| application    | `SessionUseCase` and `SessionService`; ports `TokenIssuerPort`, `UserProvisioningPort`                                |
| infrastructure | `KeycloakTokenIssuerAdapter`, `AuthController`, `BrowserSessions`, `SessionTokenRelayFilter`, the ACL to `useraccess` |

`useraccess` is the only neighbour, reached through `infrastructure/acl`.

## Decisions worth knowing

### Keycloak owns every page that touches a credential

The module never sees a password. Keycloak's pages carry its brute-force protection,
password policy, e-mail verification, terms of use and two-factor setup, which the old
password grant bypassed or made the application re-implement. The realm allows no password
grant for `freshly-app-client`, only the authorization code flow with PKCE.

With e-mail verification on, Keycloak 26.7 asks for no password at registration: the account
is created, the terms are accepted, the verification link is sent, and the password is set
after the link is opened.

### State and PKCE are tied to the browser that started

`/auth/authorize` stores the state and the code verifier in the server-side session, and the
callback takes them out once. A callback whose state was not issued to this browser is
refused, so a code obtained elsewhere cannot be planted in someone's session. After a
successful exchange the pre-login session is invalidated and a new one is created, so a
session identifier known before sign-in is worthless after it.

### Provisioning happens at sign-in, and undoes the session if it fails

`SessionService.signIn` exchanges the code and creates the `useraccess` record when there is
none. If the record cannot be written, the refresh token Keycloak just issued is revoked, so a
person is never signed in without the account the rest of the application expects.

Roles come from the access token, minus Keycloak's built-ins (`default-roles-*`,
`offline_access`, `uma_authorization`).

### The relay refuses cross-site writes

The session cookie is `SameSite=Lax`, which already keeps it off cross-site `POST`, `PUT`,
`PATCH` and `DELETE`. Lax still treats another subdomain of the same site as same-site, so the
relay also reads `Sec-Fetch-Site`: an unsafe method sent from anywhere other than this origin
gets no token and is answered 401.

### One refresh at a time per session

Keycloak rotates refresh tokens and refuses a reused one, so two requests refreshing the same
session at once would sign the person out. Each session holds its own lock; the second request
waits and then uses the token the first one obtained. A refresh Keycloak refuses ends the
session; one that fails because Keycloak is unreachable leaves it for the next request.

### Sessions live in memory

The deployment runs one replica, and a restart signs everyone out, which for this application
is acceptable. More replicas would need a shared session store (Spring Session on MongoDB) —
until then the session attribute holds a small `Serializable` snapshot, so adding one does not
change this module.

## Testing

| Tier        | Where                                    |
|-------------|------------------------------------------|
| Domain      | `AuthSessionTest`: refresh timing, masked `toString` |
| Application | `SessionServiceTest`: provisioning, revocation when provisioning fails |
| Integration | `bootstrap/.../HostedSignInTest`: authorize, state check, sign-in with provisioning, relay, refresh failure, cross-site write, logout |
