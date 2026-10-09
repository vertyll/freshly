# Account provisioning

How a person gets an account the first time they sign in.

`SignInCompletion` runs after Spring has the tokens: it reads the person from the access token
and `SessionService.signIn` creates the `useraccess` record when there is none. If the token is
unusable or the record cannot be written, the refresh token Keycloak just issued is revoked and
the session is ended, so a person is never signed in without the account the rest of the
application expects.

Roles come from the access token, minus Keycloak's built-ins (`default-roles-*`,
`offline_access`, `uma_authorization`).
