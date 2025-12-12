plugins {
	java
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
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

dependencies {
	// Internal dependencies
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

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

	// JUnit
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
