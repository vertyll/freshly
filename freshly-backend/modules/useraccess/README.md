# useraccess

Owns a person's standing inside the application: are they active, and what roles
do they hold.

**This is the module to copy.** The other five follow this shape;
clone it rather than inventing a variant.

## What it owns

| Concern                                       | Where                                                  |
|-----------------------------------------------|--------------------------------------------------------|
| The `SystemUser` aggregate and its invariants | `useraccess-domain/model`                              |
| What this context can refuse, and why         | `useraccess-domain/error`                              |
| The permissions it declares                   | `useraccess-application/security/UserAccessPermission` |
| What it needs from storage                    | `useraccess-domain/repository`                         |
| Its public API                                | `useraccess-application/port/inbound`                  |
| MongoDB, HTTP, Spring wiring                  | `useraccess-infrastructure`                            |

## What it deliberately does not own

**Credentials, e-mail, profile.** Those are Keycloak's. This aggregate joins to
them by `keycloakUserId` and has no password column by design, which is what keeps
the context small enough to state its invariants in one class.

**Provisioning.** `auth` decides when a user should exist — at their first sign-in — and
calls `UserAccessCommandUseCase.createUser`. This module reaches Keycloak only through
`RoleDirectoryPort`, for roles and for whether an account may sign in.

**Which permissions a role grants.** `permission` stores that. This module only
declares which permissions it will enforce.

## Decisions worth knowing

### Self-deactivation is refused by the aggregate, not the use case

`SystemUser.deactivate(actorId)` takes the actor and refuses when it matches. It
would work in `UserAccessCommandService`, and it would have to be repeated at
every call site added later — including one reached from another module's ACL,
where nobody would think to look.

The actor comes from the JWT subject claim, never the request body. A caller who
could name the actor could name somebody else and route around the rule entirely.

### A user with no roles is not constructible

Not "validated against" — not constructible. `requireNonBlankRoles` runs in the
private constructor, so `create`, `reconstitute` and `replaceRoles` all go through
it. That is what makes "authenticated implies at least one role" a fact the rest
of the application can rely on rather than a convention.

A user with no roles would authenticate and then be refused everything, which
looks to the person like an outage rather than a decision.

### `getUser` throws, `findUser` returns empty

Both exist because the callers differ in kind. A controller answering
`GET /users/{id}` wants the refusal to become a 404 without writing the branch.
`auth`'s provisioning adapter is asking *in order to decide*, and for it "absent"
is an answer rather than a failure.

### The permission enum has a `Values` twin

An annotation argument must be a constant expression, so
`@RequirePermission(UserAccessPermission.USERS_READ.value())` does not compile.
`UserAccessPermission.Values` holds the same strings as `static final` fields.

That duplication is a real cost and `UserAccessPermissionTest` is what stops it
becoming a bug: it asserts by reflection that the two sets match exactly. Rename
one without the other and the test fails rather than the endpoint silently
becoming unreachable.

## Mechanisms

- [Identity provider sync](docs/mechanisms/identity-provider-sync.md) – What this module changes in Keycloak, and why
  Keycloak stays the source of truth.
- [Queries](docs/mechanisms/queries.md) – How user lists and lookups are read.

## Testing

| Tier                       | Where                                | Needs                            |
|----------------------------|--------------------------------------|----------------------------------|
| Domain                     | `useraccess-domain/src/test`         | nothing                          |
| Application                | `useraccess-application/src/test`    | nothing                          |
| Architecture / integration | `useraccess-infrastructure/src/test` | ArchUnit; Docker for integration |

The first two run without Spring, without a database and without a container —
which is the entire return on the framework-free rule, and the reason
`UserAccessCommandServiceTest` constructs its subject with `new`.

`InMemorySystemUserRepository` is a real implementation of the port rather than a
Mockito mock. A mock asserts which methods were called; these tests care what the
data looks like afterwards, and a fake also catches a use case that saves twice or
forgets to save at all.
