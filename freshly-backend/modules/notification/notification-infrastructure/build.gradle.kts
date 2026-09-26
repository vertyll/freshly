plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:notification:notification-application"))

    implementation(libs.bundles.spring.boot.starters.mail)
}
