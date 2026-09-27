package com.vertyll.freshly.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Rules that govern the platform rather than any one bounded context.
 *
 * <p>
 * Run by {@code PlatformArchitectureTest} in {@code shared-archunit}'s own test
 * source set, so the platform is checked on every build like any module.
 */
public final class PlatformArchitectureRules {
    private static final String LANG_PACKAGES = "com.vertyll.freshly.lang..";

    /**
     * Listed rather than written as one alternation.
     *
     * <p>
     * ArchUnit turns a package identifier into a regular expression, so
     * {@code "com.vertyll.freshly.(lang|authz).."} happens to work — by accident, through a
     * translation that is free to change, and parentheses already mean something else in
     * that syntax. A rule that silently matched nothing would pass, which is the one failure
     * mode an architecture test must not have.
     */
    private static final String[] PLATFORM = {
        LANG_PACKAGES,
        "com.vertyll.freshly.authz..",
        "com.vertyll.freshly.i18n..",
        "com.vertyll.freshly.infra..",
        "com.vertyll.freshly.web..",
        "com.vertyll.freshly.security..",
        "com.vertyll.freshly.archunit.."
    };

    private static final String[] MODULES = {
        "com.vertyll.freshly.useraccess..",
        "com.vertyll.freshly.notification..",
        "com.vertyll.freshly.permission..",
        "com.vertyll.freshly.airquality..",
        "com.vertyll.freshly.auth..",
        "com.vertyll.freshly.translation.."
    };

    /** The framework-free platform modules, which an application layer may depend on. */
    private static final String[] FRAMEWORK_FREE_PLATFORM = {
        LANG_PACKAGES,
        "com.vertyll.freshly.authz..",
        "com.vertyll.freshly.i18n.."
    };

    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "jakarta..",
        "org.hibernate..",
        "com.fasterxml.jackson..",
        "tools.jackson..",
        "org.slf4j..",
        "org.mongodb..",
        "com.mongodb..",
        "org.mapstruct..",
        "lombok..",
        "io.jsonwebtoken..",
        "org.keycloak.."
    };

    private PlatformArchitectureRules() {
    }

    /**
     * The rule easiest to break without noticing.
     *
     * <p>
     * {@code shared-security} has to ask whether a caller holds a permission, and
     * depending on {@code permission-application} to do it looks reasonable. It makes the
     * platform unbuildable without a bounded context, drags {@code permission} into any
     * second application built on this platform, and creates a latent cycle the moment
     * {@code permission-infrastructure} needs anything from {@code shared-security}.
     *
     * <p>
     * {@code PermissionEvaluator} inverts it — the platform declares the SPI, the
     * context implements it. This rule is what stops the shortcut being taken.
     */
    public static ArchRule platformDoesNotDependOnAnyModule() {
        return noClasses().that()
            .resideInAnyPackage(PLATFORM)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(MODULES)
            .because(
                "the platform is what modules are built on; a platform that needs a "
                        + "bounded context is a bounded context wearing a platform's name"
            );
    }

    /**
     * No framework reaches the framework-free platform.
     *
     * <p>
     * {@code shared-lang} and {@code shared-authz} carry nothing but the JDK;
     * {@code shared-i18n} adds ICU4J, which is a formatting library in the same category as
     * {@code java.time} — no container, no lifecycle, nothing to configure.
     *
     * <p>
     * Already checked by {@code checkHexagonalDependencies} at the classpath level.
     * Restated here because the classpath check can only see artifacts, and this catches
     * the case where a framework arrives through a module that is itself allowed.
     */
    public static ArchRule frameworkFreePlatformStaysFrameworkFree() {
        return noClasses().that()
            .resideInAnyPackage(FRAMEWORK_FREE_PLATFORM)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because(
                "an application layer depends on these, so what arrives here arrives "
                        + "on the inside of every hexagon in the application"
            );
    }

    /**
     * {@code shared-lang} holds no domain concept.
     *
     * <p>
     * Weak as a mechanical rule — a class name cannot prove a type is technical — so it
     * checks the one property that is checkable: nothing in there names a business concept
     * from the ubiquitous language. The real enforcement is the admission criterion written
     * in the module's build file, and review.
     *
     * <p>
     * The names below are the terms a context owns and the platform must not. Extend
     * the list when a new context introduces one worth protecting.
     */
    public static ArchRule sharedLanguageHoldsNoDomainConcept() {
        return noClasses().that()
            .resideInAPackage(LANG_PACKAGES)
            .should()
            .haveSimpleNameContaining("User")
            .orShould()
            .haveSimpleNameContaining("Permission")
            .orShould()
            .haveSimpleNameContaining("Email")
            .orShould()
            .haveSimpleNameContaining("Station")
            .orShould()
            .haveSimpleNameContaining("Measurement")
            .orShould()
            .haveSimpleNameContaining("Role")
            .because(
                "a shared module with no admission criteria becomes the place things "
                        + "go when nobody wants to decide where they belong — which is what " + "happened to `common`"
            );
    }

    /**
     * Configuration properties belong to the module that reads them.
     *
     * <p>
     * {@code common} held {@code KeycloakProperties}, {@code JwtProperties} and
     * {@code MailProperties}, so the identity-provider admin secret, the token signing key
     * and the SMTP password were on the compile path of every module — including the ones
     * that draw charts. Nothing abused it; nothing prevented it either.
     *
     * <p>
     * {@code CorsProperties} is the single exception, because CORS genuinely is a
     * platform concern with no owning context.
     */
    public static ArchRule platformHoldsNoModuleConfiguration() {
        return classes().that()
            .resideInAnyPackage(PLATFORM)
            .and()
            .areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
            .should()
            .haveSimpleName("CorsProperties")
            .because(
                "a secret belongs to the one module that needs it, not to everything "
                        + "that happens to depend on the platform"
            );
    }

    /**
     * The Spring-bound platform modules are not reachable from inside a hexagon.
     *
     * <p>
     * {@code shared-web}, {@code shared-security} and {@code shared-infra} are for
     * adapters. A use case naming {@code ApiResponse} or {@code TransactionalUseCaseFactory}
     * has reached outward, and the classpath check would already have failed — this states
     * the intent at class level so the failure names the right thing.
     */
    public static ArchRule springPlatformIsForAdaptersOnly() {
        return noClasses().that()
            .resideInAnyPackage("com.vertyll.freshly.*.domain..", "com.vertyll.freshly.*.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.vertyll.freshly.web..",
                "com.vertyll.freshly.security..",
                "com.vertyll.freshly.infra.."
            )
            .because(
                "the Spring-bound platform serves adapters; the inside of a hexagon "
                        + "reaches it through a port or not at all"
            );
    }

    public static void check(JavaClasses classes) {
        platformDoesNotDependOnAnyModule().check(classes);
        frameworkFreePlatformStaysFrameworkFree().check(classes);
        sharedLanguageHoldsNoDomainConcept().check(classes);
        platformHoldsNoModuleConfiguration().check(classes);
        springPlatformIsForAdaptersOnly().check(classes);
    }
}
