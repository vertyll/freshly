# useraccess

Owns a person's standing inside the application: are they active, and what roles
do they hold.

**This is the module to copy.** The other five follow this shape;
clone it rather than inventing a variant.

## What it owns

| Concern            | Where                                            |
|--------------------|--------------------------------------------------|
| The `SystemUser` aggregate and its invariants | `useraccess-domain/model`     |
| What this context can refuse, and why         | `useraccess-domain/error`     |
| The permissions it declares                   | `useraccess-application/security/UserAccessPermission` |
| What it needs from storage                    | `useraccess-domain/repository` |
| Its public API                                | `useraccess-application/port/inbound` |
| MongoDB, HTTP, Spring wiring                  | `useraccess-infrastructure`   |

## What it deliberately does not own

**Credentials, e-mail, profile.** Those are Keycloak's. This aggregate joins to
them by `keycloakUserId` and has no password column by design, which is what keeps
the context small enough to state its invariants in one class.

**Provisioning.** `auth` decides when a user should exist and calls
`UserAccessCommandUseCase.createUser`. This module does not know Keycloak exists.

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

### CQRS at the port level, not the model level

`UserAccessCommandUseCase` and `UserAccessQueryUseCase` are separate; there is no
separate read model. Every query here returns a single aggregate with nothing to
assemble across boundaries, so a projection would be a second place to maintain
for no measurable gain.

The split still earns its keep: `TransactionalUseCaseFactory` reads the
transaction mode from which port it is, so a method added to the query side cannot
accidentally run read-write.

Revisit this when a query has to span `useraccess` and `permission`.

### Paging happens in the database

The port takes a `PageRequest` and the adapter pages in MongoDB with an explicit sort.
Returning every user and letting the client cope is fine at a hundred users and nothing
beyond; and without the explicit sort, Mongo does not promise to return rows in the same
order twice, so page 2 can repeat a row from page 1.

### The permission enum has a `Values` twin

An annotation argument must be a constant expression, so
`@RequirePermission(UserAccessPermission.USERS_READ.value())` does not compile.
`UserAccessPermission.Values` holds the same strings as `static final` fields.

That duplication is a real cost and `UserAccessPermissionTest` is what stops it
becoming a bug: it asserts by reflection that the two sets match exactly. Rename
one without the other and the test fails rather than the endpoint silently
becoming unreachable.

## Testing

| Tier        | Where                              | Needs   |
|-------------|------------------------------------|---------|
| Domain      | `useraccess-domain/src/test`       | nothing |
| Application | `useraccess-application/src/test`  | nothing |
| Architecture / integration | `useraccess-infrastructure/src/test` | ArchUnit; Docker for integration |

The first two run without Spring, without a database and without a container —
which is the entire return on the framework-free rule, and the reason
`UserAccessCommandServiceTest` constructs its subject with `new`.

`InMemorySystemUserRepository` is a real implementation of the port rather than a
Mockito mock. A mock asserts which methods were called; these tests care what the
data looks like afterwards, and a fake also catches a use case that saves twice or
forgets to save at all.

## Roles are the identity provider's, not ours

`PUT /users/{id}/roles` goes through `RoleDirectoryPort`: the names are checked against the
realm, the provider is updated, and only then is the local copy saved. That copy is a
projection for reading and listing; it is not what is enforced.

Writing role names onto the local document and stopping there changes nothing that matters.
Authorization reads roles out of the token, and a token carries Keycloak's realm roles — the
administrator assigns a role, the screen shows it, and access stays as it was. A name with a
typo would be accepted just as readily.

`GET /roles` lists what the realm offers, so a panel has something to choose from rather than
a free-text box.

The order inside the use case is deliberate: the aggregate's own rule runs first, so an empty
set costs no round trip; then the realm check; then the provider; then the save. A failure at
any step leaves the previous ones either undone or harmless — the local write is last, and it
is the only one inside the transaction.
