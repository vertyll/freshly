plugins {
    base
    `jacoco-report-aggregation`
    id("com.diffplug.spotless")
    id("org.sonarqube")
}

description = "Air Quality Monitoring System - API"

repositories {
    mavenCentral()
}

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

data class DocumentedLibrary(val name: String, val blurb: String, val frameworkFree: Boolean)

val documentedLibraries = listOf(
    DocumentedLibrary("shared-lang", "Errors, paging, version guard and the ports every module shares", true),
    DocumentedLibrary("shared-authz", "The permission vocabulary and the authorization SPI", true),
    DocumentedLibrary("shared-i18n", "ICU message patterns: writing, checking and rendering them", true),
    DocumentedLibrary("shared-infra", "Transaction proxy, use-case logger and cache manager", false),
    DocumentedLibrary("shared-web", "Problem details, ETag handling, the paging response and the web annotations", false),
    DocumentedLibrary("shared-security", "Filter chain, method security and role extraction", false),
    DocumentedLibrary("shared-archunit", "The architecture rules every bounded context is checked against", false)
)

tasks.register("docs") {
    group = "documentation"
    description = "Generates the Javadoc of the platform libraries with a landing page (build/docs/javadoc/index.html)"

    val libraries = documentedLibraries
    dependsOn(libraries.map { ":platform:${it.name}:javadoc" })

    val site = layout.buildDirectory.dir("docs/javadoc")
    val template = layout.projectDirectory.file("gradle/docs/landing.html")
    inputs.file(template)
    val sources = libraries.associate { it.name to project(":platform:${it.name}").layout.buildDirectory.dir("docs/javadoc") }

    doLast {
        val root = site.get().asFile
        root.deleteRecursively()
        sources.forEach { (name, dir) -> dir.get().asFile.copyRecursively(root.resolve(name)) }

        fun cards(entries: List<DocumentedLibrary>) =
            entries.joinToString("\n") {
                """      <li><a href="${it.name}/index.html"><code>${it.name}</code></a><span>${it.blurb}</span></li>"""
            }

        val landingPage = root.resolve("index.html")
        landingPage.writeText(
            template.asFile
                .readText()
                .replace("@@PROJECT@@", "freshly")
                .replace("@@SUBJECT@@", "platform")
                .replace("@@HEADING@@", "platform libraries")
                .replace("@@LEDE@@", "API documentation generated from Javadoc.")
                .replace("@@PURE_SCOPE@@", "safe for a domain or application layer")
                .replace("@@UNIT@@", "library")
                .replace("@@PURE@@", cards(libraries.filter { it.frameworkFree }))
                .replace("@@SPRING@@", cards(libraries.filterNot { it.frameworkFree }))
        )
        logger.lifecycle("Javadoc: ${landingPage.toURI()}")
    }
}

dependencies {
    jacocoAggregation(platform(libs.spring.boot.dependencies))
    subprojects.filter { it.buildFile.exists() }.forEach { jacocoAggregation(it) }
}

reporting {
    reports {
        register<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName = "test"
        }
    }
}

val aggregatedCoverage = layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required = true
    }
}

subprojects {
    sonar {
        properties {
            property("sonar.coverage.jacoco.xmlReportPaths", aggregatedCoverage.get().asFile.path)
        }
    }
}

tasks.named("sonar") {
    dependsOn("testCodeCoverageReport")
}

sonar {
    properties {
        property("sonar.projectKey", "freshly")
        property("sonar.projectName", "freshly")
        property("sonar.issue.ignore.multicriteria", "emailTables,emailAttributes,localSecrets,uploadLimitsYaml,jjwtDates")
        property("sonar.issue.ignore.multicriteria.emailTables.ruleKey", "Web:S5257")
        property("sonar.issue.ignore.multicriteria.emailTables.resourceKey", "**/templates/**/*.html")
        property("sonar.issue.ignore.multicriteria.emailAttributes.ruleKey", "Web:S1827")
        property("sonar.issue.ignore.multicriteria.emailAttributes.resourceKey", "**/templates/**/*.html")
        property("sonar.issue.ignore.multicriteria.localSecrets.ruleKey", "java:S6437")
        property("sonar.issue.ignore.multicriteria.localSecrets.resourceKey", "**/application-local.*")
        property("sonar.issue.ignore.multicriteria.uploadLimitsYaml.ruleKey", "java:S5693")
        property("sonar.issue.ignore.multicriteria.uploadLimitsYaml.resourceKey", "**/application*.yml")
        property("sonar.issue.ignore.multicriteria.jjwtDates.ruleKey", "java:S2143")
        property("sonar.issue.ignore.multicriteria.jjwtDates.resourceKey", "**/*Jwt*.java")
    }
}
