plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
    api(project(":modules:common"))

    // API
    api(libs.bundles.spring.boot.starters.common)
    api(libs.spring.boot.starter.security)
    api(libs.mapstruct)

    // Compile Only
    compileOnly(libs.lombok)

    // Annotation Processor
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.bundles.mapstruct.processors)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.bundles.mapstruct.processors)

    // Test Implementation
    testImplementation(libs.bundles.spring.boot.test.common)
    testImplementation(libs.spring.boot.starter.security.test)
}
