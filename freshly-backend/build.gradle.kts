plugins {
    base
    id("com.diffplug.spotless")
}

description = "Air Quality Monitoring System - API"

spotless {
    format("buildLogic") {
        target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/**/*.gradle.kts", "buildSrc/src/**/*.kt")
        targetExclude("**/build/**")
        replaceRegex(
            "IDE-generated Kotlin DSL accessor import",
            "(?m)^import gradle\\.kotlin\\.dsl\\.accessors\\._[0-9a-f]+\\.\\*\\R",
            ""
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}

val innerLayers = subprojects.filter {
    it.name.endsWith("-domain") || it.name.endsWith("-application")
}

tasks.register("checkHexagonalDependencies") {
    group = "verification"
    description = "Fails if a framework reaches the domain or application layer of any module"
    dependsOn(innerLayers.map { "${it.path}:checkHexagonalDependencies" })
}

tasks.register("checkArchitecture") {
    group = "verification"
    description = "Runs the classpath check and every architecture test in the build"
    dependsOn("checkHexagonalDependencies")

    dependsOn(
        subprojects
            .filter { it.name.endsWith("-infrastructure") }
            .map { "${it.path}:test" }
    )

    dependsOn(":bootstrap:test")
}

tasks.named("check") {
    dependsOn("checkHexagonalDependencies")
}
