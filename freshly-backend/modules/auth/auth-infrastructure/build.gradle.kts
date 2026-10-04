plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:auth:auth-application"))

    implementation(project(":modules:useraccess:useraccess-application"))
    implementation(project(":platform:shared-security"))

    implementation(libs.jackson.annotations)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.spring.boot.starter.security.oauth2.client)
}
