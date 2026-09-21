plugins {
    id("freshly.infrastructure-layer")
}

dependencies {
    api(project(":modules:airquality:airquality-application"))
    implementation(project(":modules:airquality:airquality-domain"))

    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.jackson.annotations)
}
