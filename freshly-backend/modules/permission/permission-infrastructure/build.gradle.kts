plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:permission:permission-application"))
    implementation(project(":modules:permission:permission-domain"))

    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.webmvc)
}
