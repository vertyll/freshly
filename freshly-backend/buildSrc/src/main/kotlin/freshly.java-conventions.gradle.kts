import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    pmd
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
    id("com.github.spotbugs")
}

group = "com.vertyll"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

configure<DependencyManagementExtension> {
    imports {
        mavenBom(libs.findLibrary("spring-boot-dependencies").get().get().toString())
    }
}

dependencies {
    compileOnly(libs.findLibrary("jspecify").get())
    compileOnly(libs.findLibrary("spotbugs-annotations").get())

    annotationProcessor(libs.findLibrary("guava-beta-checker").get())

    add("errorprone", libs.findLibrary("errorprone-core").get())
    add("errorprone", libs.findLibrary("nullaway").get())

    add("spotbugsPlugins", libs.findLibrary("findsecbugs").get())

    testCompileOnly(libs.findLibrary("jspecify").get())
    testCompileOnly(libs.findLibrary("spotbugs-annotations").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")

    options.errorprone {
        enabled.set(true)

        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
        option("NullAway:CustomContractAnnotations", "org.springframework.lang.Contract")
        option("NullAway:ExcludedFieldAnnotations", "lombok.Generated")
        option("NullAway:TreatGeneratedAsUnannotated", "true")
        option("NullAway:ExternalInitAnnotations", "org.springframework.data.mongodb.core.mapping.Document")
        option("NullAway:AcknowledgeRestrictiveAnnotations", "true")
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")

        excludedPaths.set(".*/build/generated/.*")
    }
}

configure<SpotBugsExtension> {
    toolVersion.set(libs.findVersion("spotbugs").get().requiredVersion)
    ignoreFailures.set(false)
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.LOW)
    showProgress.set(true)
    excludeFilter.set(rootProject.file("config/spotbugs/exclude-filter.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    val projectName = project.name
    val taskName = name
    val reportDir = project.layout.buildDirectory.dir("reports/spotbugs")

    reports.maybeCreate("html").apply {
        required.set(true)
        outputLocation.set(reportDir.map { it.file("$projectName-$taskName.html") })
        setStylesheet("fancy-hist.xsl")
    }

    reports.maybeCreate("xml").apply {
        required.set(true)
        outputLocation.set(reportDir.map { it.file("$projectName-$taskName.xml") })
    }
}

tasks.named("check") {
    dependsOn("spotbugsMain")
}

configure<SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        targetExclude("**/build/generated/**/*.java", "**/*Impl.java")

        removeUnusedImports()

        importOrder(
            "java", "javax", "jakarta", "org", "com.vertyll", "com", "",
            "\\#java", "\\#javax", "\\#jakarta", "\\#org", "\\#com.vertyll", "\\#com", "\\#"
        )

        eclipse(libs.findVersion("eclipse-jdt").get().requiredVersion)
            .configFile(rootProject.file("config/formatter/eclipse-java-custom-style.xml"))

        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn()
    }

    format("gradle") {
        target("*.gradle.kts", "**/*.gradle.kts")
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

pmd {
    isConsoleOutput = true
    toolVersion = libs.findVersion("pmd").get().requiredVersion
    ruleSets = listOf()
    ruleSetFiles = files(rootProject.file("config/pmd/pmd-main-ruleset.xml"))
    isIgnoreFailures = false
}

tasks.withType<Pmd>().configureEach {
    if (name == "pmdTest") {
        ruleSetFiles = files(rootProject.file("config/pmd/pmd-test-ruleset.xml"))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

NullMarkedPackageCheck.register(project)
