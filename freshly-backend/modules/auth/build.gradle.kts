plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
    api(project(":modules:common"))
    api(project(":modules:notification"))
    api(project(":modules:useraccess"))

    // API
    api(libs.bundles.spring.boot.starters.common)
    api(libs.bundles.spring.boot.starters.oauth)
    api(libs.mapstruct)

    // Implementation
    api(libs.jjwt.api)
    implementation(libs.keycloak.admin.client)
    implementation(libs.httpclient5)

    // Compile Only
    compileOnly(libs.lombok)

    // Runtime Only
    runtimeOnly(libs.bundles.jjwt)

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
    testImplementation(libs.spring.boot.starter.security.oauth2.resource.server.test)
}
