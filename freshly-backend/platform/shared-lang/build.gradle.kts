/**
 * Framework-free technical building blocks.
 *
 * # Why this is not called `shared-kernel`
 *
 * In DDD a Shared Kernel is a *domain model* two contexts agree to share, and Evans
 * describes it as the integration pattern demanding the tightest coordination between
 * teams — something to reach for reluctantly. This module is not that. There is no
 * domain concept in here: `DomainError`, `PageRequest` and `VersionGuard` are technical
 * vocabulary, the equivalent of a standard library, and depending on them commits a
 * module to nothing about how it models its own subject.
 *
 * The name would promise a much stronger coupling than exists, and — more practically —
 * a module called Shared Kernel is one people feel entitled to put domain concepts in.
 * That is how a shared module turns into the node everything passes through.
 *
 * # What may go in here
 *
 * A type earns a place only when **two or more contexts genuinely need the same one**,
 * and only when it carries no domain meaning. "Could plausibly be shared" is not the
 * bar: `Email` stayed in `notification` even though `auth` also handles addresses,
 * because they are not the same concept — one is a delivery target, the other an
 * identity attribute.
 *
 * Enforced by `PlatformArchitectureTest`, and by the fact that this module's only
 * dependency is JSpecify.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(libs.jspecify)
    testImplementation(libs.bundles.test.unit)
}

HexagonalClasspathCheck.register(project, "shared language")
