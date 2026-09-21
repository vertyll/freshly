/**
 * Spring Security wiring, which is why it sits in `platform/` and not in `modules/`.
 *
 * There is no domain here, no aggregate and nothing to decide — only the filter chain,
 * the authorization managers, the argument resolvers and the role extraction. Listing it
 * beside `airquality` and `useraccess` would make the module list a poor guide to what
 * the application is about.
 *
 * Nothing that relaxes security belongs in `src/main`. A configuration written to make
 * tests easier is on the production classpath from the moment it is placed there.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))
    api(project(":platform:shared-web"))

    api(libs.spring.boot.starter.security)
    api(libs.bundles.spring.boot.starters.oauth)
    implementation(libs.spring.boot.starter.webmvc)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.spring.boot.test.common)
}
