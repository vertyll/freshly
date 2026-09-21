package com.vertyll.freshly.authz;

/**
 * Which level of the application a permission is decided at.
 *
 * <p>
 * Freshly currently has only {@link #GLOBAL}: every administrative capability
 * is either held or not, with no resource whose attributes could narrow it. The
 * enum exists anyway because it is the difference between a permission model that
 * can grow a second level and one that has to be rewritten to get it — and
 * because a permission that names its scope cannot be granted to a role that
 * enforces a different one.
 */
public enum PermissionScope {

    /** May this person administer the application? Plain RBAC. */
    GLOBAL,

    /** May this person do this to <em>this</em> resource? Attribute-based. */
    RESOURCE
}
