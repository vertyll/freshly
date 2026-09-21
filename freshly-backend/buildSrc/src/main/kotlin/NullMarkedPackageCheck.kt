import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

object NullMarkedPackageCheck {

    private const val PACKAGE_INFO = "package-info.java"

    fun register(project: Project): TaskProvider<*> {
        val task = project.tasks.register("checkNullMarkedPackages") {
            group = "verification"
            description = "Asserts that every package with production code declares @NullMarked."

            val sourceRoot = project.file("src/main/java")
            val projectPath = project.path

            doLast {
                if (!sourceRoot.isDirectory) {
                    return@doLast
                }

                val unmarked = packagesWithCode(sourceRoot)
                    .filter { !File(it, PACKAGE_INFO).exists() }
                    .map { it.relativeTo(sourceRoot).path.replace(File.separatorChar, '.') }
                    .sorted()

                if (unmarked.isNotEmpty()) {
                    throw GradleException(
                        buildString {
                            appendLine("$projectPath: ${unmarked.size} package(s) carry code but no @NullMarked:")
                            unmarked.forEach { appendLine("  $it") }
                            appendLine()
                            appendLine("NullAway only checks what JSpecify marks, and a package does not mark its")
                            appendLine("subpackages. Add a $PACKAGE_INFO to each:")
                            appendLine()
                            appendLine("    @NullMarked")
                            appendLine("    package <name>;")
                            appendLine()
                            appendLine("    import org.jspecify.annotations.NullMarked;")
                        }
                    )
                }
            }
        }

        project.tasks.named("check") { dependsOn(task) }
        return task
    }

    private fun packagesWithCode(sourceRoot: File): List<File> =
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".java") && it.name != PACKAGE_INFO }
            .map { it.parentFile }
            .distinct()
            .toList()
}
