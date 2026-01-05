plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
    api(project(":modules:common"))
    api(project(":modules:permission"))

    // API
    api(libs.spring.boot.starter.security)
    api(libs.bundles.spring.boot.starters.oauth)
    api(libs.spring.boot.starter.webmvc)

    // Implementation
    implementation(libs.jjwt.api)

    // Compile Only
    compileOnly(libs.lombok)

    // Runtime Only
    runtimeOnly(libs.bundles.jjwt)

    // Annotation Processor
    annotationProcessor(libs.lombok)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)

    // Test Implementation
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.oauth2.resource.server.test)
}
