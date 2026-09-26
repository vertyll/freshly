plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))
    api(project(":platform:shared-web"))

    api(libs.spring.boot.starter.security)
    api(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.spring.boot.starter.webmvc)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.spring.boot.test.common)
}
