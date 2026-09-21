/**
 * How a message pattern is written, checked and rendered.
 *
 * The one place ICU is named. Everything else — the web platform rendering a problem
 * detail, the `translation` context refusing an override that will not compile — goes
 * through [com.vertyll.freshly.i18n.IcuMessages] and stays ignorant of which formatter
 * is behind it.
 *
 * Separate from `shared-lang` on purpose. `shared-lang` carries nothing but JSpecify, and
 * it is on every domain and application classpath in the application; putting a formatting
 * library there would widen that surface for the five contexts that never format anything.
 *
 * Framework-free, so `checkHexagonalDependencies` guards it like the other two: ICU4J is a
 * formatting library in the same category as `java.time`, not a framework.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))
    api(libs.icu4j)

    testImplementation(libs.bundles.test.unit)
}

HexagonalClasspathCheck.register(project, "shared i18n")
