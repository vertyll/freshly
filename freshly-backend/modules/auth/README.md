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

`useraccess` is the only neighboring module, reached through `infrastructure/acl`. The filter
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

## Mechanisms

- [Account provisioning](docs/mechanisms/account-provisioning.md) – How a person gets an account the first time they
  sign in.
- [Cross-site requests](docs/mechanisms/cross-site-requests.md) – Why a forged request from another site cannot act with
  the user's session.
- [Sign-in state](docs/mechanisms/sign-in-state.md) – How a callback is matched to the sign-in that started it.
- [Token audience](docs/mechanisms/token-audience.md) – Which access tokens the API accepts.
- [Token refresh](docs/mechanisms/token-refresh.md) – How the session keeps a valid access token without signing the
  user out when requests race.

## Testing

| Tier        | Where                                                                                                                                                                                   |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Domain      | `SignedInUserTest`: the roles are copied                                                                                                                                                |
| Application | `SessionServiceTest`: provisioning, revocation when provisioning fails                                                                                                                  |
| Integration | `bootstrap/.../SharedRefreshesTest`: one refresh across replicas, on a Redis container                                                                                                  |
| Integration | `bootstrap/.../HostedSignInTest`: authorize, state check, sign-in with provisioning, refresh, refused refresh, bearer token, cross-site write, logout — on MongoDB and Redis containers |
