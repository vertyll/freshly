plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
    api(project(":modules:common"))

    // API
    api(libs.bundles.spring.boot.starters.common)
    api(libs.mapstruct)

    // Implementation
    implementation(libs.httpclient5)

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
}
