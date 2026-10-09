# Known limitations

These are properties of the design, not defects to be repaired quietly.

## The caches are single-instance-correct only

`ConcurrentMapCache`, evicted locally, for both role grants and resolved translations. With
two instances, revoking a grant or correcting a translation on instance A leaves instance B
serving the old answer until it restarts. Redis is already part of the deployment for
sessions, so the fix is a change to the platform's `CacheConfig` — and it is a correctness
bug the moment the deployment scales, not a performance one.

## Assigning a role writes to two systems with no transaction across them

`replaceUserRoles` updates Keycloak and then the local projection, inside the database
transaction. If the commit fails after Keycloak succeeded, the person's real access has
changed and the copy this application lists has not.

The ordering makes the survivable case the common one — the provider is what a token
carries, so it is what matters, and the projection is only what the admin screen lists. The
way out is not a distributed transaction: it is reading roles from the provider when listing
a user and keeping no copy.

## One installation, one set of everything

No tenant discriminator anywhere: one set of translation overrides, one set of role grants,
one measurement history. Adding one changes the identity of `TranslationKey` and
`RoleAuthority` and needs a migration of both collections.
