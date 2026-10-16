package com.vertyll.freshly.infra.cache;

/**
 * A cache one bounded context needs, declared by that context.
 *
 * <p>
 * Spring needs exactly one primary {@code CacheManager}. Two modules each building
 * their own over their own caches would either fail to start or — worse, depending on
 * resolution order — start with one module's caches registered and the other's
 * silently absent. A {@code @Cacheable} naming a cache that does not exist throws at the
 * first call, which is a start-up problem discovered at request time.
 *
 * <p>
 * The shape is the same as {@code PermissionCatalogue}: the
 * platform owns the single manager and the contract, each context contributes a bean
 * saying what it needs, and no context knows about any other's caches.
 *
 * @param name the cache name, which the module's {@code @Cacheable} must match; prefix
 *     it with the context so two modules cannot collide on a generic name
 */
public record CacheSpec(String name) {

    private static final String NAME_BLANK = "Cache name cannot be blank";

    public CacheSpec {
        if (name.isBlank()) {
            throw new IllegalArgumentException(NAME_BLANK);
        }
    }
}
