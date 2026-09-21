/**
 * The innermost layer of a bounded context.
 *
 * Contains aggregates, value objects, domain services (policies), repository
 * *ports* and the context's error catalogue. Nothing here may know how anything
 * is stored, transported or serialized — the domain is the one layer that has to
 * outlive any framework choice the rest of the application makes.
 *
 * Its entire allowed dependency surface is the JDK plus `shared-lang`.
 * Lombok is deliberately absent: an aggregate with a generated all-args
 * constructor and generated setters is a data holder, not an aggregate, and the
 * annotation is exactly what makes that easy to do by accident.
 */

plugins {
    id("freshly.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":platform:shared-lang"))

    testImplementation(libs.findBundle("test-unit").get())
}

HexagonalClasspathCheck.register(project, "domain layer")
