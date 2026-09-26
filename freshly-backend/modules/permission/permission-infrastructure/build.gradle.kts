plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:permission:permission-application"))

    implementation(libs.spring.boot.starter.security)
}
