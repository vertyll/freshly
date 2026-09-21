plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:translation:translation-application"))
    implementation(project(":modules:translation:translation-domain"))

    implementation(project(":platform:shared-i18n"))

    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.spring.boot.starter.webmvc)

    implementation(libs.poi.ooxml)
}
