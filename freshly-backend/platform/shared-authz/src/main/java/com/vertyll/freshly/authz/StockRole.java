package com.vertyll.freshly.authz;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A role a module ships with, and what it grants on an installation that has none.
 *
 * <p>
 * Applied only where the role holds nothing yet. Once it does, an administrator owns it,
 * so deploying the module again never restores a permission somebody deliberately took away
 * — which is the same distinction the translation defaults draw against overrides, for the
 * same reason: a system that quietly undoes an administrator's decision stops being one
 * anybody edits.
 *
 * @param unrestricted holds every permission in the application, including those of a module
 *     added later. The administrator is expressed this way rather than as a
 *     list, so a module that ships tomorrow is covered on the day it ships
 *     rather than when somebody remembers to tick its boxes.
 */
public record StockRole(String role, boolean unrestricted, Set<String> permissions) {

    private static final Pattern ROLE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    public StockRole {
        requireNonNull(role, "Stock role name cannot be null");
        if (!ROLE_FORMAT.matcher(role).matches()) {
            throw new IllegalArgumentException("Role name '" + role + "' must be UPPER_SNAKE_CASE");
        }
        permissions = Set.copyOf(requireNonNull(permissions, "Stock role permissions cannot be null"));

        // An unrestricted role holds everything, so a list alongside the flag is a list
        // nothing reads — and the author of it believes those are the permissions granted.
        if (unrestricted && !permissions.isEmpty()) {
            throw new IllegalArgumentException(
                "Unrestricted role '" + role + "' must not also name permissions: " + permissions
            );
        }
    }

    /** An unrestricted role, for the administrator. */
    public static StockRole unrestricted(String role) {
        return new StockRole(role, true, Set.of());
    }

    public static StockRole granting(String role, String... permissions) {
        // copyOf, not Set.of: a module naming one permission twice is a harmless typo,
        // and Set.of would turn it into a boot failure naming neither role nor value.
        return new StockRole(role, false, Set.copyOf(Arrays.asList(permissions)));
    }
}
