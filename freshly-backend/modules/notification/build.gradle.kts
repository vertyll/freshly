plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
    api(project(":modules:common"))

    // API
    api(libs.bundles.spring.boot.starters.mail)
    api(libs.spring.boot.starter.data.mongodb)

    // Compile Only
    compileOnly(libs.lombok)

    // Annotation Processor
    annotationProcessor(libs.lombok)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)

    // Test Implementation
    testImplementation(libs.bundles.spring.boot.test.mail)
    testImplementation(libs.spring.boot.starter.data.mongodb.test)
}
