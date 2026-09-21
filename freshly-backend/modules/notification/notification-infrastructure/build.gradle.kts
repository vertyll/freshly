plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:notification:notification-application"))
    implementation(project(":modules:notification:notification-domain"))

    implementation(libs.bundles.spring.boot.starters.mail)
}
