/**
 * The permission vocabulary, framework-free.
 *
 * The platform owns only the *contract* — `PermissionDescriptor`, `PermissionCatalogue`,
 * `StockRole`, `CallerRoles`, `PermissionEvaluator`. Each bounded context declares its
 * own catalogue in its own domain layer, and `permission` stores role-to-permission
 * mappings by string without knowing any catalogue at all.
 *
 * A single shared enum holding every context's permissions would make any module with a
 * guarded endpoint depend on it, and adding a permission to `airquality` would mean
 * editing a file `useraccess` also compiles against.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(libs.jspecify)
    testImplementation(libs.bundles.test.unit)
}

HexagonalClasspathCheck.register(project, "shared authz")
