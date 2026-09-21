/**
 * The architecture rules every bounded context is checked against, as executable
 * tests.
 *
 * A module applies them with one class:
 *
 * ```java
 * class UserAccessArchitectureTest extends FreshlyArchitectureTest {
 *     UserAccessArchitectureTest() { super("com.vertyll.freshly.useraccess"); }
 * }
 * ```
 *
 * This is a *test* library, so it declares its dependencies as `api`: the
 * consuming project puts it on `testImplementation` and needs JUnit and ArchUnit
 * to come with it.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(libs.archunit.junit5)
    api(libs.junit.jupiter)
}
