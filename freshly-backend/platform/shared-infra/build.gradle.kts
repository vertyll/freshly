/**
 * Technical infrastructure with no domain content, shared by every module's
 * infrastructure layer.
 *
 * # Why these are shared rather than one copy per context
 *
 * "Infrastructure is a module's own" argues for a copy of `Slf4jUseCaseLogger` and
 * `TransactionalUseCaseFactory` in each bounded context, and for keeping a platform
 * module off every context's compile path. Neither half holds.
 *
 * Every `*-infrastructure` already depends on `shared-web`, so the compile-path cost is
 * zero. And a module owning its infrastructure means owning the *choices* — which
 * database, which mail transport, which HTTP client. It does not mean owning a private
 * copy of a reflection proxy that is byte-for-byte identical everywhere.
 *
 * The decisive argument is maintenance. `TransactionalUseCaseFactory` unwraps
 * `InvocationTargetException` so a `DomainException` does not reach the exception handler
 * disguised as a reflection failure. That is subtle and easy to get wrong, and duplicated
 * it is a fix that has to land three times with nothing to notice if one is missed.
 * Identical technical code copied N times is not modularity.
 *
 * # What may go in here
 *
 * Only code that is (a) pure technical infrastructure, (b) genuinely identical across
 * modules, and (c) carries no knowledge of any context. Anything a module might want to
 * configure differently stays in that module: `GiosRestClientConfig` is infrastructure
 * too, and belongs to `airquality` alone.
 *
 * Depended on by `*-infrastructure` projects only. Never by an application or domain
 * layer — `checkHexagonalDependencies` would fail the build.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    api(libs.spring.tx)
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.spring.boot.test.common)
}
