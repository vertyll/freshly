plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:auth:auth-application"))

    implementation(project(":modules:useraccess:useraccess-application"))
    implementation(project(":modules:notification:notification-application"))

    implementation(libs.keycloak.admin.client)
    implementation(libs.jackson.annotations)
    implementation(libs.bundles.jjwt)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.bundles.spring.boot.starters.oauth)
}
