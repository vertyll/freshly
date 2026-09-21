plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

/**
 * The convention plugins need the same plugins on their own classpath in order to
 * configure them. Versions are duplicated from `gradle/libs.versions.toml` because
 * a version catalog is not visible inside `buildSrc`'s own build script by default.
 */
dependencies {
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.1")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:5.1.1")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:3.2.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.11")
}
