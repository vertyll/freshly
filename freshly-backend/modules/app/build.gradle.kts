plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Implementation - Internal Modules
    implementation(project(":modules:common"))
    implementation(project(":modules:auth"))
    implementation(project(":modules:security"))
    implementation(project(":modules:permission"))
    implementation(project(":modules:useraccess"))
    implementation(project(":modules:notification"))
    implementation(project(":modules:airquality"))

    // Implementation - Spring Boot Starters
    implementation(libs.bundles.spring.boot.starters.common)
    implementation(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.bundles.spring.boot.starters.aop)

    // Implementation - MapStruct
    implementation(libs.mapstruct)

    // Compile Only
    compileOnly(libs.lombok)

    // Runtime Only
    runtimeOnly(libs.spring.boot.devtools)

    // Development Only
    developmentOnly(libs.spring.boot.docker.compose)

    // Annotation Processor
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.bundles.mapstruct.processors)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)

    // Test Implementation
    testImplementation(libs.bundles.test.starters)
    testImplementation(libs.bundles.testcontainers)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = false
}
