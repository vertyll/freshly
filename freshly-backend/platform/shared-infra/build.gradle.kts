plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    api(libs.spring.tx)
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.spring.boot.test.common)
}
