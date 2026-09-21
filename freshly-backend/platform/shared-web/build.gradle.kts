/**
 * The Spring-bound half of the platform: HTTP delivery and nothing else.
 *
 * One rule decides membership: a type belongs here if it is about *delivering* a
 * response over HTTP — problem details, ETag handling, the paging response shape,
 * the web annotations. Framework-free helpers live in `shared-lang`, the permission
 * contract in `shared-authz`, and a `*Properties` class belongs to the infrastructure
 * of the single module that reads it.
 *
 * The rule is what keeps this from becoming the node everything passes through. A
 * shared module holding HTTP plumbing, an authorization vocabulary and every
 * context's configuration properties makes each module depend on all three.
 */
plugins {
    id("freshly.java-conventions")
    `java-library`
}

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))
    api(project(":platform:shared-i18n"))

    api(libs.spring.boot.starter.webmvc)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.starter.security)
    api(libs.spring.tx)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.spring.boot.test.common)
}
