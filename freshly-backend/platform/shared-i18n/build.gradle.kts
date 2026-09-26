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
