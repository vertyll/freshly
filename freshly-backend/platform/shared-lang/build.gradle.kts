plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(libs.jspecify)
    testImplementation(libs.bundles.test.unit)
}

HexagonalClasspathCheck.register(project, "shared language")
