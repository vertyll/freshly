package com.vertyll.freshly.authz;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every permission one bounded context declares.
 *
 * <p>
 * Each context contributes one of these at start-up. The permission module
 * assembles them into the set of grants an administrator may choose from, which
 * is what lets it validate "this role may not be granted a permission nobody
 * declared" without compiling against any context's enum.
 *
 * <p>
 * Announcing the catalogue unconditionally at every start-up, rather than only
 * when it changed, is deliberate: a module deployed after the store was seeded
 * would otherwise never register, and every grant naming its permissions would be
 * silently refused.
 */
public interface PermissionCatalogue {

    /** The context these permissions belong to, matching {@link PermissionDescriptor#context()}. */
    String context();

    Set<PermissionDescriptor> permissions();

    /**
     * Roles this context ships with, if any.
     *
     * <p>
     * Validated at start-up against {@link #permissions()}: a stock role may not grant
     * what its own module does not declare. Most contexts ship none — a role is an
     * application-wide idea, and only the context that owns authorization has an opinion
     * about which ones should exist out of the box.
     */
    default Set<StockRole> stockRoles() {
        return Set.of();
    }

    default Set<String> values() {
        return permissions().stream().map(PermissionDescriptor::value).collect(Collectors.toUnmodifiableSet());
    }

    static Optional<PermissionDescriptor> find(Collection<PermissionCatalogue> catalogues, String value) {
        return catalogues.stream()
            .flatMap(catalogue -> catalogue.permissions().stream())
            .filter(permission -> permission.value().equals(value))
            .findFirst();
    }
}
