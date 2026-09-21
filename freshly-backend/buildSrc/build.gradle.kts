plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.1")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:5.1.1")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:3.2.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.11")
}
