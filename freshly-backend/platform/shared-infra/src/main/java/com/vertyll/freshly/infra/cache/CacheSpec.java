package com.vertyll.freshly.infra.cache;

/**
 * A cache one bounded context needs, declared by that context.
 *
 * <p>
 * This SPI exists because of a real collision. Both {@code permission} and
 * {@code airquality} defined their own {@code CacheManager} bean, each building a
 * {@code SimpleCacheManager} over its own caches. Spring needs exactly one primary
 * {@code CacheManager}, so the context would either fail to start or — worse, depending
 * on resolution order — start with one module's caches registered and the other's
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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(NAME_BLANK);
        }
    }
}
