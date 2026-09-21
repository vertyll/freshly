# auth

Registration, sessions and credentials. Delegates identity to Keycloak; owns the flows
around it.

The most demanding module in the application: three external systems, no transaction
spanning them, and the only flow that has to undo its own work.

## The shape

### Six ports, no concrete collaborators

The registration use case names what it needs and nothing more:
`IdentityProviderPort`, `TokenIssuerPort`, `VerificationTokenPort`, `PasswordCheckPort`,
plus `UserProvisioningPort` and `UserNotificationPort` for the two neighbouring
contexts. The last two are satisfied in `infrastructure/acl`, where an anti-corruption
adapter translates between this module's language and theirs.

Nothing in the application layer sees Keycloak's object model — no
`List<UserRepresentation>` crossing a port — so the use cases are testable with fakes
and no Spring context.

### The application layer speaks its own commands

Inbound ports take command records declared in `auth-application`, not the web adapter's
request DTOs. Those DTOs carry `jakarta.validation` annotations describing an HTTP
contract, and the hexagon's core must not depend on the delivery mechanism;
`applicationDoesNotDependOnWeb` enforces it.

### Cryptography is an adapter

`JwtVerificationTokenAdapter` in `infrastructure/token` signs and parses tokens; it is
the only place `io.jsonwebtoken` appears. The domain keeps what is actually domain
knowledge: `TokenPurpose`, `VerificationToken`, and the rule that using a token for the
wrong purpose is a refusal.

### No auth use case is wrapped in a transaction

Every operation here is against Keycloak or SMTP, neither of which participates in a
database transaction. A `@Transactional` boundary around them would commit nothing and
would suggest that `registerUser` is atomic, when what actually provides safety is the
compensation stack below. `ApplicationBeansConfig` records the decision at the point
where the other modules' use cases are wrapped and these are not.

## Registration and its compensation

This is the only place in the application that spans systems which cannot share a
transaction: Keycloak over HTTP, MongoDB via `useraccess`, SMTP.

Compensations are pushed onto a stack as they become necessary and unwound in reverse,
so how far the rollback reaches depends on how far registration got.
`RegistrationServiceTest` asserts the order and the end state.

The failure this shape exists to prevent: undoing only the Keycloak user would leave the
`useraccess` record behind — an account record pointing at an identity that no longer
exists, invisible in Keycloak, and enough to make a retry with the same address fail on a
duplicate nobody can see.

**What it still cannot promise.** A compensating action can itself fail — Keycloak may be
the thing that is down. When that happens the failure is logged at error and the
*original* exception still reaches the caller, because the caller needs to know
registration failed more than it needs to know the cleanup was untidy. That leaves an
orphan, and it is the honest boundary: reliable cleanup needs a durable record of intent,
which a transactional outbox would provide and this application does not have.

## Decisions worth knowing

### A verification mail failure is fatal; a welcome mail failure is not

Both go through the same module and the same transport. Registration treats the first as
fatal because an account nobody can verify is worse than no account — the address is now
taken and the person cannot try again. The welcome message is fire-and-forget, because a
missing one costs nothing.

This is exactly why `notification` reports failures instead of deciding for its callers, and
why `UserNotificationPort` declares the two differently. The promise is kept in
`NotificationDispatchAdapter`: `@Async` so a slow SMTP server does not hold the registration
response open, and the failure caught there, because an exception thrown from an async method
has no caller left to observe it.

### The welcome message is a direct call, not an event

`RegistrationService` calls `UserNotificationPort` itself. The test for which channel to
use is who owns the reaction: "send a welcome message after registration" is `auth`'s
policy — `notification` sends mail and should not know what registration is. Publishing
an event that a listener in `auth-infrastructure` consumes would put publisher and
reactor in the same module, which decouples nothing and hides what registration triggers
from anyone reading it.

### `TokenPurpose` is checked, and the check is load-bearing

Verification tokens and reset tokens are signed with the same key and carry the same
subject. Without the purpose claim, anyone holding a verification link — a forwarded
e-mail, say — could reset that account's password.

One validation method takes the expected purpose as a parameter rather than one method
per purpose. Near-identical copies differing only in a constant and a message key drift,
and a drifted check here is a silent hole.

### `initiatePasswordReset` returns silently for unknown addresses

Carried over deliberately, and flagged in the code, because it reads exactly like a
missing error branch. Answering differently for a known and an unknown address turns the
endpoint into an account-enumeration oracle.

### The refresh token travels in an HttpOnly cookie

It never appears in the JSON body. In the body, any script on the page can read the one
credential that outlives an access token, and the front end has to store it somewhere —
in practice `localStorage`. The cookie is scoped to `/auth`, `SameSite=Lax` so an e-mail
link navigation does not drop it.

### Keycloak statuses are translated

A 409 from the admin API becomes `USERNAME_ALREADY_EXISTS` or `EMAIL_ALREADY_EXISTS` —
the adapter checks the address itself, because Keycloak does not say which field
collided. A 401 from the token endpoint becomes `INVALID_CREDENTIALS`.

Left as a generic client exception, both surface as a 500, and a duplicate registration
reads as a server fault rather than something to fix in the form.

### The signing key is derived with an explicit UTF-8

Not `Charset.defaultCharset()`, which differs between a laptop and a container: a secret
with any non-ASCII byte would produce a different key in each, and tokens minted by one
instance would fail validation on another.

### `searchByEmail` is exact

Keycloak substring-matches by default, so `a@b.com` also finds `aa@b.com`. In a
password-reset flow that mails the link to the wrong person.

### Two Keycloak clients, not one

`IdentityProviderPort` uses a privileged service account; `TokenIssuerPort` uses the
public client with the user's own credentials. One class holding both makes it easy to
reach an administrative operation from a request path carrying only a user token.

### `KeycloakPasswordCheck` is a documented workaround

Keycloak has no "is this password correct" endpoint, so verification is a throwaway
password grant. Isolated in its own class because it is a workaround rather than an API
call, and because the session it opens must be closed — a leaked one holds a valid
refresh token.

Only a 4xx from the token endpoint reads as a wrong password. Anything else — a 5xx, a
connection that never opened — is `IDENTITY_PROVIDER_UNAVAILABLE`: the change is still
refused, but the person is told the service failed rather than that they mistyped.
