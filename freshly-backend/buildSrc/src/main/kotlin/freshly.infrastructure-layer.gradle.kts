/**
 * The outside edge of a bounded context: adapters, and the Spring wiring that
 * connects them to the inside.
 *
 * Everything the other two layers were not allowed to touch lives here — web
 * controllers and their request DTOs, MongoDB documents and repositories, HTTP
 * clients, the anti-corruption adapters that call another module's use cases,
 * the bean configuration and the transactional decorator.
 *
 * This project is a plain library, not an application. `bootJar` is disabled
 * across the build and only `:bootstrap` produces a runnable artifact: there is
 * one process, so there is one composition root.
 */

plugins {
    id("freshly.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))
    implementation(project(":platform:shared-infra"))
    implementation(project(":platform:shared-web"))

    implementation(libs.findBundle("spring-boot-starters-common").get())
    implementation(libs.findLibrary("mapstruct").get())

    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findBundle("mapstruct-processors").get())

    testCompileOnly(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findBundle("mapstruct-processors").get())

    testImplementation(libs.findBundle("spring-boot-test-common").get())
    testImplementation(project(":platform:shared-archunit"))
}
