import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * Asserts that every package carrying production code declares `@NullMarked`.
 *
 * NullAway runs in `OnlyNullMarked` mode, so what it checks is exactly what JSpecify has
 * marked — and JSpecify does not treat a package as containing its subpackages, because
 * Java packages are not nested. A `package-info.java` at `…useraccess.domain` therefore
 * says nothing about `…useraccess.domain.model`, which is where the code actually lives.
 *
 * Without this check the failure is silent and the worst kind: the compiler runs NullAway,
 * NullAway finds no annotated code to reason about, and the build goes green. It looks
 * exactly like a codebase with no nullness bugs.
 *
 * A file check rather than a bytecode one because `@NullMarked` has CLASS retention, so a
 * test could not see it by reflection, and because the thing being asserted is about the
 * source layout: a package somebody adds tomorrow must not fall out of coverage quietly.
 */
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
