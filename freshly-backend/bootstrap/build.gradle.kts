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
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.session.data.redis)
    if (System.getProperty("os.name").startsWith("Mac")) {
        val arch = if (System.getProperty("os.arch") == "aarch64") "osx-aarch_64" else "osx-x86_64"
        runtimeOnly(variantOf(libs.netty.resolver.dns.native.macos) { classifier(arch) })
    }
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    developmentOnly(libs.spring.boot.devtools)

    testImplementation(libs.bundles.test.starters)
    testImplementation(libs.spring.boot.starter.security.oauth2.client)
    testImplementation(project(":platform:shared-archunit"))
    testImplementation(libs.bundles.testcontainers)
}

tasks.bootJar {
    archiveFileName.set("freshly-backend.jar")
}

tasks.jar {
    enabled = false
}
