import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    pmd
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.nullaway) apply false
}

group = "com.vertyll"
version = "0.0.1-SNAPSHOT"
description = "Air Quality Monitoring System - API"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("pmd")
        plugin("io.spring.dependency-management")
        plugin("com.diffplug.spotless")
        plugin("net.ltgt.errorprone")
        plugin("net.ltgt.nullaway")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(rootProject.libs.spring.boot.dependencies.get().toString())
        }
    }

    dependencies {
        // Compile Only
        compileOnly(rootProject.libs.jspecify)

        // Annotation Processor
        annotationProcessor(rootProject.libs.guava.beta.checker)

        // Error Prone
        add("errorprone", rootProject.libs.errorprone.core)
        add("errorprone", rootProject.libs.nullaway)

        // Test Runtime Only
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
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
        }

        val ansiReset = "\u001B[0m"
        val ansiGreen = "\u001B[32m"
        val ansiRed = "\u001B[31m"
        val ansiYellow = "\u001B[33m"
        val ansiCyan = "\u001B[36m"
        val ansiBold = "\u001B[1m"

        val checkMark = "✓"
        val crossMark = "✗"
        val skipMark = "⊘"

        afterTest(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
            val indicator = when (result.resultType) {
                TestResult.ResultType.SUCCESS -> "$ansiGreen$checkMark$ansiReset"
                TestResult.ResultType.FAILURE -> "$ansiRed$crossMark$ansiReset"
                TestResult.ResultType.SKIPPED -> "$ansiYellow$skipMark$ansiReset"
                else -> "?"
            }
            val duration = result.endTime - result.startTime
            println("  $indicator ${desc.className} > ${desc.name} $ansiCyan(${duration}ms)$ansiReset")
        }))

        afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
            if (desc.parent == null) {
                val total = result.testCount
                val passed = result.successfulTestCount
                val failed = result.failedTestCount
                val skipped = result.skippedTestCount
                val duration = result.endTime - result.startTime

                println()
                println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
                println("$ansiBold                        TEST RESULTS                        $ansiReset")
                println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
                println()
                println("  Total:   $ansiBold$total$ansiReset tests")
                println("  Passed:  $ansiGreen$ansiBold$passed$ansiReset $ansiGreen$checkMark$ansiReset")
                println("  Failed:  $ansiRed$ansiBold$failed$ansiReset ${if (failed > 0) "$ansiRed$crossMark$ansiReset" else ""}")
                println("  Skipped: $ansiYellow$ansiBold$skipped$ansiReset ${if (skipped > 0) "$ansiYellow$skipMark$ansiReset" else ""}")
                println()
                println("  Duration: $ansiCyan${duration}ms$ansiReset")
                println()

                val statusColor = when (result.resultType) {
                    TestResult.ResultType.SUCCESS -> ansiGreen
                    TestResult.ResultType.FAILURE -> ansiRed
                    else -> ansiYellow
                }
                println("  Status: $statusColor$ansiBold${result.resultType}$ansiReset")
                println()
                println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
                println()
            }
        }))
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/main/java/**/*.java", "src/test/java/**/*.java")
            targetExclude("**/build/generated/**/*.java", "**/*Impl.java")

            googleJavaFormat(rootProject.libs.versions.google.java.format.get()).aosp()

            removeUnusedImports()
            importOrder("java", "javax", "org", "com", "com.vertyll")

            trimTrailingWhitespace()
            endWithNewline()

            toggleOffOn()
        }

        format("gradle") {
            target("*.gradle.kts", "**/*.gradle.kts")
            trimTrailingWhitespace()
            leadingTabsToSpaces(4)
            endWithNewline()
        }
    }

    pmd {
        isConsoleOutput = true
        toolVersion = rootProject.libs.versions.pmd.get()
        ruleSets = listOf()
        ruleSetFiles = files(rootProject.file("config/pmd/pmd-main-ruleset.xml"))
        isIgnoreFailures = false
    }

    tasks.withType<Pmd> {
        if (name == "pmdTest") {
            ruleSetFiles = files(rootProject.file("config/pmd/pmd-test-ruleset.xml"))
        }
    }
}

tasks.register<TestReport>("testReport") {
    group = "verification"
    description = "Generate aggregated test report for all modules"
    destinationDirectory.set(layout.buildDirectory.dir("reports/all-tests"))

    testResults.from(subprojects.map { it.tasks.withType<Test>() })

    doLast {
        val reportFile = destinationDirectory.get().file("index.html").asFile
        println("\nAggregated test report generated:")
        println("   file://${reportFile.absolutePath}\n")

        val os = System.getProperty("os.name").lowercase()
        try {
            when {
                os.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", reportFile.absolutePath))
                }

                os.contains("nix") || os.contains("nux") -> {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", reportFile.absolutePath))
                }

                os.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", reportFile.absolutePath))
                }
            }
            println("Opening report in browser...\n")
        } catch (e: Exception) {
            println("Could not open browser automatically: ${e.message}\n")
        }
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Run all tests in all modules and generate aggregated report"

    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    finalizedBy("testReport")
}
