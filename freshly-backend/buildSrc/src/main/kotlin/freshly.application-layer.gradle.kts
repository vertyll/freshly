plugins {
    id("freshly.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))

    testImplementation(libs.findBundle("test-unit").get())
}

HexagonalClasspathCheck.register(project, "application layer")
