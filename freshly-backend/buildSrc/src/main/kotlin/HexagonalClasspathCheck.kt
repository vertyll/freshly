import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

object HexagonalClasspathCheck {

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
