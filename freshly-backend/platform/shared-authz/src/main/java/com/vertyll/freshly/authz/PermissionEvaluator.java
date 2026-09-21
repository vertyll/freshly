package com.vertyll.freshly.authz;

import java.util.Set;

/**
 * Answers whether a set of roles grants a permission.
 *
 * <p>
 * This interface exists to keep an arrow pointing the right way, and the arrow is
 * worth explaining because the wrong direction looks perfectly reasonable.
 *
 * <p>
 * {@code shared-security} has to enforce {@code @RequirePermission} — that is Spring
 * Security plumbing, it is identical for every module, and it belongs in the platform.
 * But the <em>answer</em> lives in the {@code permission} bounded context. The obvious
 * wiring is for {@code shared-security} to depend on {@code permission-application} and
 * call its query port.
 *
 * <p>
 * That is wrong in a way that is easy to miss. It makes the platform depend on a
 * bounded context, so the platform cannot be understood or compiled without one; it
 * means adding a second application on this platform drags {@code permission} along; and
 * it creates a latent cycle the moment {@code permission-infrastructure} needs anything
 * from {@code shared-security}.
 *
 * <p>
 * With this SPI the platform declares what it needs, {@code permission-infrastructure}
 * supplies it as a bean, and the dependency runs context → platform like every other one.
 * It is the same shape as {@link PermissionCatalogue} in the other direction — the
 * platform holds the contract, the context holds the behaviour.
 *
 * <p>
 * Framework-free, so an application layer could also depend on it without pulling
 * Spring in.
 */
public interface PermissionEvaluator {

    boolean permits(CallerRoles roles, String permission);

    boolean permitsAny(CallerRoles roles, Set<String> permissions);

    /** Everything this caller may do, for a client rendering controls from it. */
    Set<String> permissionsOf(CallerRoles roles);
}
