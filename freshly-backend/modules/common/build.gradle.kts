plugins {
    id("java-library")
}

sourceSets {
    main {
        resources {
            srcDir("../app/src/main/resources")
        }
    }
    test {
        resources {
            srcDir("../app/src/main/resources")
        }
    }
}

dependencies {
    // API
    api(libs.bundles.spring.boot.starters.common)
    api(libs.mapstruct)

    // Compile Only
    compileOnly(libs.lombok)

    // Annotation Processor
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.mapstruct.processor)

    // Test Compile Only
    testCompileOnly(libs.lombok)

    // Test Annotation Processor
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.mapstruct.processor)

    // Test Implementation
    testImplementation(libs.bundles.spring.boot.test.common)
}
