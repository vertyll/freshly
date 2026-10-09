# Permission lookup

How a permission check finds what a caller's roles grant, quickly and without a transaction.

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

## The query port implements a platform SPI

`PermissionQueryUseCase extends PermissionEvaluator`. That interface belongs to
`shared-authz`, and it is what lets `shared-security` enforce `@RequirePermission` without
depending on this module.

The direction matters: a platform depending on `permission-application` also works, and
it makes the platform unbuildable without a bounded context, drags this module into any
second application built on the platform, and leaves a latent cycle.

## The query port is not wrapped in a transaction

Every other module's query port goes through `TransactionalUseCaseFactory` read-only.
This one does not: `permits` runs on every guarded request, and opening a transaction
to read a cached set would cost a connection per request for nothing.
