plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:useraccess:useraccess-application"))

    implementation(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    implementation(libs.keycloak.admin.client)
}
