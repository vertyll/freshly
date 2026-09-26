plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:airquality:airquality-application"))

    implementation(libs.jackson.annotations)
}
