plugins {
    id("freshly.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))
    implementation(project(":platform:shared-infra"))
    implementation(project(":platform:shared-web"))

    implementation(libs.findBundle("spring-boot-starters-common").get())
    implementation(libs.findLibrary("mapstruct").get())

    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findBundle("mapstruct-processors").get())

    testCompileOnly(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findBundle("mapstruct-processors").get())

    testImplementation(libs.findBundle("spring-boot-test-common").get())
    testImplementation(project(":platform:shared-archunit"))
}
