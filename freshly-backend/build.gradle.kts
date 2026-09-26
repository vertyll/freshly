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
    val sources = libraries.associate { it.name to project(":platform:${it.name}").layout.buildDirectory.dir("docs/javadoc") }

    doLast {
        val root = site.get().asFile
        root.deleteRecursively()
        sources.forEach { (name, dir) -> dir.get().asFile.copyRecursively(root.resolve(name)) }

        fun cards(entries: List<DocumentedLibrary>) =
            entries.joinToString("\n") {
                """      <li><a href="${it.name}/index.html"><code>${it.name}</code></a><span>${it.blurb}</span></li>"""
            }

        val template =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>freshly — platform API documentation</title>
                <style>
                  :root {
                    color-scheme: light dark;
                    --fg: #1a1a1a; --muted: #5b5b5b; --line: #e2e2e2; --accent: #1f6feb;
                  }
                  @media (prefers-color-scheme: dark) {
                    :root { --fg: #e8e8e8; --muted: #a0a0a0; --line: #303030; --accent: #6aa9ff; }
                  }
                  body {
                    font-family: system-ui, -apple-system, sans-serif;
                    max-width: 46rem; margin: 0 auto; padding: 4rem 1.25rem;
                    line-height: 1.6; color: var(--fg);
                  }
                  h1 { font-size: 1.5rem; margin: 0 0 .25rem; }
                  h2 { font-size: .8rem; text-transform: uppercase; letter-spacing: .08em;
                       color: var(--muted); margin: 2.5rem 0 .75rem; font-weight: 600; }
                  p.lede { color: var(--muted); margin: 0 0 .5rem; }
                  ul { list-style: none; padding: 0; margin: 0; }
                  li { display: flex; flex-direction: column; gap: .15rem;
                       padding: .7rem 0; border-bottom: 1px solid var(--line); }
                  li span { color: var(--muted); font-size: .92rem; }
                  a { color: var(--accent); text-decoration: none; font-weight: 600; }
                  a:hover { text-decoration: underline; }
                  code { font-size: .95rem; }
                  footer { margin-top: 2.5rem; color: var(--muted); font-size: .88rem; }
                </style>
              </head>
              <body>
                <h1>freshly — platform libraries</h1>
                <p class="lede">API documentation generated from Javadoc.</p>

                <h2>Framework-free — safe for a domain or application layer</h2>
                <ul>
            @@PURE@@
                </ul>

                <h2>Spring — infrastructure layer only</h2>
                <ul>
            @@SPRING@@
                </ul>

                <footer>
                  What each library is responsible for, and why they are separate, is described in
                  <code>docs/shared-modules.md</code>.
                </footer>
              </body>
            </html>
            """.trimIndent()

        val landingPage = root.resolve("index.html")
        landingPage.writeText(
            template
                .replace("@@PURE@@", cards(libraries.filter { it.frameworkFree }))
                .replace("@@SPRING@@", cards(libraries.filterNot { it.frameworkFree }))
        )
        logger.lifecycle("Javadoc: ${landingPage.toURI()}")
    }
}
