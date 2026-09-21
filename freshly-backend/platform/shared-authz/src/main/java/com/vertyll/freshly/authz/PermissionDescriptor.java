package com.vertyll.freshly.authz;

/**
 * One capability a role may be granted.
 *
 * <p>
 * A bounded context declares its own enum implementing this — see
 * {@code UserAccessPermission} — and no context ever names another's. The
 * permission module persists grants by {@link #value()} alone, so it holds the
 * mapping without holding the catalogue.
 *
 * <p>
 * A permission is granted to a role, never to a person. Attaching permissions
 * to users directly is not RBAC: granting access becomes a list of tick boxes per
 * person, and "what can a manager do" stops having an answer.
 */
public interface PermissionDescriptor {

    /** The stable wire form, e.g. {@code users:read}. Persisted; never renamed lightly. */
    String value();

    /** Which level this permission is decided at. */
    PermissionScope scope();

    /** The context that owns it, used to reject a grant naming an unknown permission. */
    String context();

    /**
     * Translation key for the sentence an administration screen shows beside the checkbox.
     *
     * <p>
     * Derived rather than declared, for two reasons. It cannot be forgotten, and it cannot
     * drift from the permission it describes — {@code users:read} is always
     * {@code permission.users.read}, so renaming one without the other is not expressible.
     *
     * <p>
     * A key rather than a sentence because the panel is translated like everything else,
     * and because a description written in the enum would be the one piece of user-facing
     * text in the application that an administrator could not correct.
     */
    default String descriptionKey() {
        return "permission." + value().replace(':', '.');
    }
}
