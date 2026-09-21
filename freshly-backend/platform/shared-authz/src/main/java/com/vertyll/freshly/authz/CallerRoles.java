package com.vertyll.freshly.authz;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The roles a caller holds, with no framework attached.
 *
 * <p>
 * It belongs to the platform rather than to the permission context because it is the
 * <em>published language</em> of authorization: {@code shared-security} produces one from
 * a token, the permission context consumes one, and neither should have to import the
 * other to name it.
 */
public record CallerRoles(Set<String> values) {

    private static final CallerRoles ANONYMOUS = new CallerRoles(Set.of());

    public CallerRoles {
        values = values.stream()
            .map(role -> role.strip().toUpperCase(Locale.ROOT))
            .filter(role -> !role.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public static CallerRoles of(Set<String> roles) {
        return new CallerRoles(roles);
    }

    /**
     * An unauthenticated caller.
     *
     * <p>
     * One shared instance: the value is immutable and this is on the path of every
     * anonymous request.
     */
    public static CallerRoles anonymous() {
        return ANONYMOUS;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
