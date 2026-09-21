plugins {
    id("freshly.application-layer")
}

dependencies {
    api(project(":modules:translation:translation-domain"))

    testImplementation(project(":platform:shared-i18n"))
}
