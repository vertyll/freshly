plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(libs.archunit.junit5)
    api(libs.junit.jupiter)
}
