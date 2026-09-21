import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Fails the build if a framework reaches the compile classpath of a layer that is
 * supposed to be free of one.
 *
 * The hexagonal dependency rule is only worth stating if something enforces it.
 * Without this the layer stays clean exactly until the first person adds a Spring
 * starter to fix a compile error, and nothing objects.
 *
 * It reads the *resolved* `compileClasspath` rather than the declared
 * dependencies, so a framework arriving transitively is caught too. That is the
 * easy path to miss: naming a single enum from a Spring-bound module puts the
 * whole framework on this layer's classpath, and no declared-dependency check
 * would notice.
 *
 * The ArchUnit rules in `platform:shared-archunit` overlap with this on purpose.
 * This check fails earlier and with a clearer message; ArchUnit reaches what a
 * classpath structurally cannot express — a `@Document` on a domain model, a
 * controller outside the web adapter, a port declared as a class.
 */
object HexagonalClasspathCheck {

    /**
     * Package prefixes a framework-free layer may never carry.
     *
     * `org.mapstruct` and `lombok` are on the list for the same reason SLF4J is:
     * each is arguably "just" a code generator rather than a framework, but
     * admitting one exception turns the rule into a judgment call that a build
     * check can no longer make. Mapping between a domain model and a document is
     * an adapter's job, and the adapter may use MapStruct freely.
     */
    private val FORBIDDEN = listOf(
        "org.springframework",
        "jakarta.persistence",
        "jakarta.validation",
        "jakarta.servlet",
        "org.hibernate",
        "com.fasterxml.jackson",
        "tools.jackson",
        "org.slf4j",
        "org.apache.logging",
        "org.mongodb",
        "org.mapstruct",
        "org.projectlombok",
        "io.jsonwebtoken",
        "org.keycloak",
        "org.apache.httpcomponents",
        "org.apache.poi"
    )

    fun register(project: Project, layerDescription: String): TaskProvider<*> {
        val task = project.tasks.register("checkHexagonalDependencies") {
            group = "verification"
            description = "Asserts that no framework is on the $layerDescription compile classpath."

            val classpath = project.configurations.named("compileClasspath")

            doLast {
                val offenders = classpath.get()
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .asSequence()
                    .map { it.moduleVersion.id }
                    .filter { id -> FORBIDDEN.any { id.group.startsWith(it) } }
                    .map { "${it.group}:${it.name}" }
                    .distinct()
                    .sorted()
                    .toList()

                if (offenders.isNotEmpty()) {
                    throw GradleException(
                        buildString {
                            appendLine("The $layerDescription must not depend on a framework.")
                            appendLine("Found on its compile classpath:")
                            offenders.forEach { appendLine("  - $it") }
                            appendLine()
                            appendLine("Move the framework concern into `*-infrastructure` and express")
                            appendLine("it as a port. See docs/hexagonal-layering.md.")
                        }
                    )
                }
            }
        }

        project.tasks.named("check") { dependsOn(task) }
        return task
    }
}
