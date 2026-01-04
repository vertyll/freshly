import net.ltgt.gradle.errorprone.errorprone

plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("net.ltgt.errorprone") version "4.3.0"
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

dependencies {
	// Internal dependencies
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	
	// AspectJ for custom security annotations
	implementation("org.springframework:spring-aop")
	implementation("org.aspectj:aspectjweaver")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Keycloak
	implementation("org.keycloak:keycloak-admin-client:26.0.7")

	// Apache HTTP Client (required by Keycloak admin client)
	implementation("org.apache.httpcomponents.client5:httpclient5:5.6")

	// Thymeleaf and Spring Security integration
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

	// MapStruct
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	// Ensure Lombok and MapStruct work together during annotation processing
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Error Prone and NullAway
	"errorprone"("com.google.errorprone:error_prone_core:2.36.0")
	"errorprone"("com.uber.nullaway:nullaway:0.12.14")

	// Runtime
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

	// Docker Compose support
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	// Testing dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")

	// Lombok for tests
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	// Testcontainers
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:mongodb:$testcontainersVersion")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")

	// JUnit
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")

	options.errorprone.isEnabled.set(true)

	options.errorprone {
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
		events("passed", "skipped", "failed")
		showStandardStreams = false
		showExceptions = true
		showCauses = true
		showStackTraces = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
	
	afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
		if (desc.parent == null) {
			println("\nTest Results: ${result.resultType}")
			println("Tests run: ${result.testCount}, Failures: ${result.failedTestCount}, Skipped: ${result.skippedTestCount}")
		}
	}))
}
