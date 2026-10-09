# permission

Owns what each role is allowed to do, and answers whether a caller may do something.

Roles themselves live in Keycloak — a token carries them, and a token is what is enforced.
This module holds the other half: one `RoleAuthority` document per role, naming the
permissions it carries. `useraccess` owns assigning a role to a person.

## Where a permission is declared

Each context declares its own permission enum, in its application layer's `security`
package: it names what the use cases let a caller do, which is not a rule of the domain
model, so no domain layer depends on `shared-authz`. The platform owns only a contract —
`PermissionDescriptor` and `PermissionCatalogue` in `shared-authz`, framework-free —
and this module stores grants **by string** and knows no catalog.

A single enum in a shared module listing every context's capabilities would make that
module the node everything passes through: adding a permission to `airquality` would be
a change to a file `useraccess` compiles against.

The cost is that `@RequirePermission` takes a `String`, so a typo does not fail to
compile. It is bought back at runtime: saving a role refuses a permission no catalog
declared, and the seeder refuses at start-up. A typo is a failed boot, not an endpoint
nobody can reach.

Each context also exposes a nested `Values` class of `static final` strings, so a
controller names its own enum rather than writing the string by hand. A reflection
test in each context asserts the enum and the constants match.

## What the decision is made from

`CallerRoles` — a set of role names — is the only input. It lives in `shared-authz`
rather than here, because it is the published language of authorization: the platform
produces one from a token, this context consumes one, and neither should import the
other to name it.

No Spring Security type reaches the application layer. `AuthenticationRoles` in
`infrastructure/security` is the one place a token becomes role names; past it the
question is "does this set of strings grant this permission", which needs no framework
to ask and no `Authentication` to test.

## The API

The endpoints are in the Swagger UI: what the caller holds, every declared permission, and reading and replacing what
a role holds (`If-Match` on the write).

`GET /roles` lives in `useraccess` and lists the realm's roles — what exists, rather than
what each one may do. A panel needs both: one fills the left column, the other the
checkboxes.

**A description is a translation key**, not a sentence. `users:read` always describes
itself as `permission.users.read`, derived rather than declared so it cannot be forgotten
or drift, and the text lives in the owning module's `TranslationCatalogue` like everything
else an administrator reads. A build test fails if any permission has no text in some
supported language.

## Decisions worth knowing

### The cache lives on the persistence adapter

Not in the application layer. A separate cache component there exists only because
Spring's `@Cacheable` proxy is defeated by self-invocation, which is a framework mechanic
shaping the application's class structure. Three properties follow from where it sits:

**Keyed by role set, not username.** Roles change far less often than users log in,
so one entry serves everyone holding the same roles instead of one per person. It
also means creating or renaming a user needs no eviction.

**What is cached is `RoleGrants`, not the aggregate.** A flattened, immutable
`(anyUnrestricted, permissions)` rather than a list of `RoleAuthority`. Caching a mutable
aggregate hands two requests the same object for one of them to change, and this one is on
every guarded request.

**Eviction is total, and happens after the commit.** Any write clears the whole cache.
Selective eviction would mean computing which cached role sets contain the changed role,
and a grant that failed to evict is an authorization decision that stays wrong until
restart. Grants change rarely; the cache refills in one query.

The timing matters as much as the scope. A use case runs inside a transaction, so
evicting when the write method returns clears the cache while the change is still
uncommitted — and an authorization check arriving in that window refills it from the old
row and leaves it there. The adapter registers the eviction on `afterCommit` instead, and
falls back to evicting immediately when there is no transaction, which is the start-up
seeder.

**It is in-memory, so it is single-instance-correct only.** With two instances, eviction
is local and instance B keeps honoring a revoked grant. The fix is a change to the
platform's `CacheConfig` alone — worth knowing before the second instance rather than
after.

**The `CacheManager` is the platform's, not this module's.** Each context contributes a
`CacheSpec` instead of building a manager. Two modules each defining one leaves Spring
without a single primary: the application either fails to start or starts with one
module's caches registered and the other's silently absent — and a `@Cacheable` naming a
missing cache throws at the first call, not at boot.

### The query port implements a platform SPI

`PermissionQueryUseCase extends PermissionEvaluator`. That interface belongs to
`shared-authz`, and it is what lets `shared-security` enforce `@RequirePermission` without
depending on this module.

The direction matters: a platform depending on `permission-application` also works, and
it makes the platform unbuildable without a bounded context, drags this module into any
second application built on the platform, and leaves a latent cycle.

### The query port is not wrapped in a transaction

Every other module's query port goes through `TransactionalUseCaseFactory` read-only.
This one does not: `permits` runs on every guarded request, and opening a transaction
to read a cached set would cost a connection per request for nothing.

### `ADMIN` is unrestricted, not a list

`unrestricted` is a flag on the role. It holds everything the application declares,
including a module that ships next month, and there is no list for a redeploy to rebuild.

Seeding `ADMIN` with every declared permission instead is subtly wrong in a way that only
shows up once somebody uses the panel: revoke a permission from `ADMIN` and the next
restart quietly grants it back.

### Stock roles are applied only to a role that holds nothing

A module may declare the roles it ships with. They are created where the role holds nothing
yet, and never touched afterwards — past that point an administrator owns the role, and a
deployment that restored what somebody deliberately took away would be a system nobody
edits twice. It is the same distinction the translation defaults draw against overrides.

A stock role may not grant a permission its own module does not declare; the seeder refuses
at boot rather than at the first silent refusal.

### The whole set is replaced at once

`PUT /permissions/roles/{role}` takes the complete list and an `If-Match`. That is the shape
of the screen — an administrator ticks boxes and saves — and a grant-by-grant API would let
half a change land, with no version to check against.

### One `permissions:manage`, not a grant and a revoke

Separate `permissions:grant` and `permissions:revoke` cannot be enforced against a
replace-the-set endpoint: saving a shorter list *is* a revoke, and `@RequirePermission`
takes one value, so `PUT` could only ever check one of them. Two permissions where the
API can enforce one is a guard that reads as stricter than it is.

### Permissions belong to roles, never to users

There are no per-user exceptions by design. Two sources of truth mean an audit has to
consult both, and "who holds this right, and why" stops having an answer.
