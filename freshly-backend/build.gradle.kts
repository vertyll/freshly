import net.ltgt.gradle.errorprone.errorprone

plugins {
	java
    pmd
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("net.ltgt.errorprone") version "4.3.0"
    id("net.ltgt.nullaway") version "2.3.0"
}

group = "com.vertyll"
version = "0.0.1-SNAPSHOT"
description = "Spring Boot project"

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

val mapstructVersion = "1.6.3"
val testcontainersVersion = "1.21.4"
val jjwtVersion = "0.12.3"
val keycloakVersion = "26.0.7"
val httpclient5Version = "5.6"
val lombokMapstructBindingVersion = "0.2.0"
val errorProneVersion = "2.36.0"
val nullawayVersion = "0.12.14"
val betaCheckerVersion = "1.0"

dependencies {
    // Implementation dependencies
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("org.keycloak:keycloak-admin-client:$keycloakVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpclient5Version")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // Compile-only dependencies
    compileOnly("org.projectlombok:lombok")

    // Annotation processors
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("com.google.guava:guava-beta-checker:$betaCheckerVersion")

    // Runtime dependencies
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    // Development-only dependencies
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Error Prone
    errorprone("com.google.errorprone:error_prone_core:$errorProneVersion")
    errorprone("com.uber.nullaway:nullaway:$nullawayVersion")

    // Test implementation dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mongodb:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")

    // Test compile-only dependencies
    testCompileOnly("org.projectlombok:lombok")

    // Test annotation processors
    testAnnotationProcessor("org.projectlombok:lombok")

    // Test runtime dependencies
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    toolVersion = "7.20.0"
    ruleSets = listOf()
    ruleSetFiles = files("config/pmd/pmd-main-ruleset.xml")
    isIgnoreFailures = false
}

tasks.withType<Pmd> {
    if (name == "pmdTest") {
        ruleSetFiles = files("config/pmd/pmd-test-ruleset.xml")
    }
}
