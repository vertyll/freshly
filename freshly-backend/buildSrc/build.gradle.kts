plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spring.dependency.management.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.nullaway.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
    implementation(libs.sonarqube.gradle.plugin)
}
