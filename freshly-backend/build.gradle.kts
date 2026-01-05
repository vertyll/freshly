import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    pmd
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
}

group = "com.vertyll"
version = "0.0.1-SNAPSHOT"
description = "Spring Boot project"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Implementation
    implementation(libs.bundles.spring.boot.starters)
    implementation(libs.jjwt.api)
    implementation(libs.keycloak.admin.client)
    implementation(libs.httpclient5)
    implementation(libs.mapstruct)

    // Compile Only
    compileOnly(libs.lombok)

    // Annotation Processor
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.guava.beta.checker)

    // Runtime Only
    runtimeOnly(libs.bundles.jjwt)
    runtimeOnly(libs.spring.boot.devtools)

    // Development Only
    developmentOnly(libs.spring.boot.docker.compose)

    // Error Prone
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    // Test Implementation
    testImplementation(libs.bundles.test.starters)
    testImplementation(libs.bundles.testcontainers)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)

    // Test Runtime Only
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")

    options.errorprone {
        isEnabled.set(true)

        check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:CustomContractAnnotations", "org.springframework.lang.Contract")
        option("NullAway:JSpecifyMode", "true")

        excludedPaths.set(".*/build/generated/.*")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        displayGranularity = 2

        showStandardStreams = false
        showCauses = true
        showStackTraces = true
    }

    val ANSI_RESET = "\u001B[0m"
    val ANSI_GREEN = "\u001B[32m"
    val ANSI_RED = "\u001B[31m"
    val ANSI_YELLOW = "\u001B[33m"
    val ANSI_BLUE = "\u001B[34m"
    val ANSI_CYAN = "\u001B[36m"
    val ANSI_BOLD = "\u001B[1m"

    val CHECK_MARK = "✓"
    val CROSS_MARK = "✗"
    val SKIP_MARK = "⊘"

    afterTest(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        val indicator = when (result.resultType) {
            TestResult.ResultType.SUCCESS -> "$ANSI_GREEN$CHECK_MARK$ANSI_RESET"
            TestResult.ResultType.FAILURE -> "$ANSI_RED$CROSS_MARK$ANSI_RESET"
            TestResult.ResultType.SKIPPED -> "$ANSI_YELLOW$SKIP_MARK$ANSI_RESET"
            else -> "?"
        }
        val duration = result.endTime - result.startTime
        println("  $indicator ${desc.className} > ${desc.name} ${ANSI_CYAN}(${duration}ms)$ANSI_RESET")
    }))

    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        if (desc.parent == null) {
            val total = result.testCount
            val passed = result.successfulTestCount
            val failed = result.failedTestCount
            val skipped = result.skippedTestCount
            val duration = result.endTime - result.startTime

            println()
            println("$ANSI_BOLD═══════════════════════════════════════════════════════════════$ANSI_RESET")
            println("$ANSI_BOLD                        TEST RESULTS                        $ANSI_RESET")
            println("$ANSI_BOLD═══════════════════════════════════════════════════════════════$ANSI_RESET")
            println()
            println("  Total:   $ANSI_BOLD$total$ANSI_RESET tests")
            println("  Passed:  $ANSI_GREEN$ANSI_BOLD$passed$ANSI_RESET $ANSI_GREEN$CHECK_MARK$ANSI_RESET")
            println("  Failed:  $ANSI_RED$ANSI_BOLD$failed$ANSI_RESET ${if (failed > 0) "$ANSI_RED$CROSS_MARK$ANSI_RESET" else ""}")
            println("  Skipped: $ANSI_YELLOW$ANSI_BOLD$skipped$ANSI_RESET ${if (skipped > 0) "$ANSI_YELLOW$SKIP_MARK$ANSI_RESET" else ""}")
            println()
            println("  Duration: $ANSI_CYAN${duration}ms$ANSI_RESET")
            println()

            val statusColor = when (result.resultType) {
                TestResult.ResultType.SUCCESS -> ANSI_GREEN
                TestResult.ResultType.FAILURE -> ANSI_RED
                else -> ANSI_YELLOW
            }
            println("  Status: $statusColor$ANSI_BOLD${result.resultType}$ANSI_RESET")
            println()
            println("$ANSI_BOLD═══════════════════════════════════════════════════════════════$ANSI_RESET")
            println()
        }
    }))
}

pmd {
    isConsoleOutput = true
    toolVersion = libs.versions.pmd.get()
    ruleSets = listOf()
    ruleSetFiles = files("config/pmd/pmd-main-ruleset.xml")
    isIgnoreFailures = false
}

tasks.withType<Pmd> {
    if (name == "pmdTest") {
        ruleSetFiles = files("config/pmd/pmd-test-ruleset.xml")
    }
}
