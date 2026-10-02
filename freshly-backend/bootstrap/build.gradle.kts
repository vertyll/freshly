plugins {
    id("freshly.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":platform:shared-lang"))
    implementation(project(":platform:shared-authz"))
    implementation(project(":platform:shared-i18n"))
    implementation(project(":platform:shared-infra"))
    implementation(project(":platform:shared-web"))
    implementation(project(":platform:shared-security"))

    implementation(project(":modules:useraccess:useraccess-infrastructure"))
    implementation(project(":modules:permission:permission-infrastructure"))
    implementation(project(":modules:airquality:airquality-infrastructure"))
    implementation(project(":modules:translation:translation-infrastructure"))
    implementation(project(":modules:auth:auth-infrastructure"))

    implementation(libs.bundles.spring.boot.starters.common)
    implementation(libs.bundles.spring.boot.starters.aop)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    developmentOnly(libs.spring.boot.devtools)

    testImplementation(libs.bundles.test.starters)
    testImplementation(project(":platform:shared-archunit"))
    testImplementation(libs.bundles.testcontainers)
}

tasks.bootJar {
    archiveFileName.set("freshly-backend.jar")
}

tasks.jar {
    enabled = false
}
