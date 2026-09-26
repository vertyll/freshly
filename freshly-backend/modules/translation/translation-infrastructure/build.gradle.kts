plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:translation:translation-application"))

    implementation(project(":platform:shared-i18n"))

    implementation(libs.bundles.spring.boot.starters.oauth)

    implementation(libs.poi.ooxml)
}
