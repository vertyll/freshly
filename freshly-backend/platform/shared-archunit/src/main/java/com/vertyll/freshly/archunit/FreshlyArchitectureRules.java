package com.vertyll.freshly.archunit;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * The architecture rules every bounded context is held to, expressed as
 * executable checks.
 *
 * <p>
 * They restate in code what {@code docs/hexagonal-layering.md} describes in
 * prose. {@code checkHexagonalDependencies} already stops a framework from
 * reaching the inner layers' <em>classpath</em>; these rules work at class level,
 * so they catch what a classpath cannot express — a {@code @Document} on a domain
 * model, a controller outside the web adapter, a port declared as a class, or one
 * module reaching into another's internals.
 *
 * <p>
 * The two overlap deliberately. The classpath check fails earlier and with a
 * clearer message; ArchUnit covers the cases it structurally cannot reach.
 */
public final class FreshlyArchitectureRules {

    private static final String DOMAIN = "Domain";
    private static final String APPLICATION = "Application";
    private static final String INFRASTRUCTURE = "Infrastructure";
    private static final String DOMAIN_PACKAGES = ".domain..";
    private static final String APPLICATION_PACKAGES = ".application..";
    private static final String INFRASTRUCTURE_PACKAGES = ".infrastructure..";

    /** Packages a domain or application class may never touch. */
    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "jakarta.persistence..",
        "jakarta.validation..",
        "jakarta.servlet..",
        "org.hibernate..",
        "com.fasterxml.jackson..",
        "tools.jackson..",
        "org.slf4j..",
        "org.apache.logging..",
        "org.mongodb..",
        "com.mongodb..",
        "org.mapstruct..",
        "lombok..",
        "io.jsonwebtoken..",
        "org.keycloak..",
        "org.apache.poi.."
    };

    private FreshlyArchitectureRules() {
    }

    // ------------------------------------------------------------------
    // Inside one bounded context
    // ------------------------------------------------------------------

    public static ArchRule layering(String base) {
        return layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer(DOMAIN)
            .definedBy(base + DOMAIN_PACKAGES)
            .layer(APPLICATION)
            .definedBy(base + APPLICATION_PACKAGES)
            .layer(INFRASTRUCTURE)
            .definedBy(base + INFRASTRUCTURE_PACKAGES)
            .whereLayer(INFRASTRUCTURE)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(APPLICATION)
            .mayOnlyBeAccessedByLayers(INFRASTRUCTURE)
            .whereLayer(DOMAIN)
            .mayOnlyBeAccessedByLayers(APPLICATION, INFRASTRUCTURE)
            .because(
                "the dependency rule points inwards: infrastructure knows the inside, "
                        + "the inside knows nothing of it"
            );
    }

    public static ArchRule domainIsFrameworkFree(String base) {
        return noClasses().that()
            .resideInAPackage(base + DOMAIN_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because("the domain is the one layer that must outlive any framework choice");
    }

    public static ArchRule applicationIsFrameworkFree(String base) {
        return noClasses().that()
            .resideInAPackage(base + APPLICATION_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because("use cases are constructible without a container, " + "which is what makes them unit-testable");
    }

    /**
     * A request DTO carries the shape and the
     * validation constraints of one delivery mechanism, so a use case that accepts
     * one can never be driven by anything else.
     */
    public static ArchRule applicationDoesNotDependOnWeb(String base) {
        return noClasses().that()
            .resideInAnyPackage(base + APPLICATION_PACKAGES, base + DOMAIN_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(base + ".infrastructure.web..")
            .because("a request DTO is an HTTP contract; a use case takes a command instead");
    }

    /**
     * The placement rules below state where a kind of class must live if a context has
     * one. A context without controllers or documents is not a violation, so they allow
     * an empty selection; the layering rules above do not.
     */
    public static ArchRule documentsLiveInPersistence(String base) {
        return classes().that()
            .areAnnotatedWith("org.springframework.data.mongodb.core.mapping.Document")
            .should()
            .resideInAPackage(base + ".infrastructure.persistence..")
            .because("persistence is an adapter detail, not a place the domain may leak into")
            .allowEmptyShould(true);
    }

    public static ArchRule controllersLiveInWebAdapter(String base) {
        return classes().that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .resideInAPackage(base + ".infrastructure.web..")
            .because("HTTP is one delivery mechanism among several, so it belongs in its own adapter")
            .allowEmptyShould(true);
    }

    public static ArchRule inboundPortsAreInterfaces(String base) {
        return classes().that()
            .resideInAPackage(base + ".application.port..")
            .and()
            .areTopLevelClasses()
            .should()
            .beInterfaces()
            .because("a port is a contract the outside implements, never a class the inside instantiates")
            .allowEmptyShould(true);
    }

    public static ArchRule repositoryPortsAreInterfaces(String base) {
        return classes().that()
            .resideInAPackage(base + ".domain.repository..")
            .should()
            .beInterfaces()
            .because("the domain states what it needs; the adapter decides how")
            .allowEmptyShould(true);
    }

    public static ArchRule adaptersLiveInInfrastructure(String base) {
        return classes().that()
            .haveSimpleNameEndingWith("Adapter")
            .should()
            .resideInAPackage(base + INFRASTRUCTURE_PACKAGES)
            .because("an adapter is by definition the outside edge")
            .allowEmptyShould(true);
    }

    /**
     * Domain models are constructed and reconstituted by their own code, never by a
     * generated all-args constructor or a generated setter.
     *
     * <p>
     * {@code SystemUser} already gets this right by hand. The rule is what stops
     * the next aggregate from starting life as {@code @Data} and quietly becoming a
     * struct that the application layer mutates field by field.
     */
    public static ArchRule domainDoesNotUseLombok(String base) {
        return noClasses().that()
            .resideInAPackage(base + DOMAIN_PACKAGES)
            .should()
            .beAnnotatedWith("lombok.Data")
            .orShould()
            .beAnnotatedWith("lombok.Setter")
            .orShould()
            .beAnnotatedWith("lombok.Builder")
            .orShould()
            .beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould()
            .beAnnotatedWith("lombok.NoArgsConstructor")
            .because(
                "an aggregate with generated setters and an all-args constructor "
                        + "is a data holder, and its invariants are decorative"
            );
    }

    // ------------------------------------------------------------------
    // Between bounded contexts
    // ------------------------------------------------------------------

    /**
     * The rule a distributed system gets for free.
     *
     * <p>
     * When a module boundary is a process boundary the compiler enforces it: the other
     * module's classes are not on disk. In one process nothing stops a use case in one
     * context from importing another's aggregate. A module may reach another only through
     * that module's inbound ports, and only from its own infrastructure layer, where the
     * anti-corruption adapter lives.
     */
    public static ArchRule modulesDoNotReachIntoEachOthersInternals(String base, String... otherModuleBases) {
        String[] forbidden = new String[otherModuleBases.length * 2];
        for (int i = 0; i < otherModuleBases.length; i++) {
            forbidden[i * 2] = otherModuleBases[i] + DOMAIN_PACKAGES;
            forbidden[i * 2 + 1] = otherModuleBases[i] + INFRASTRUCTURE_PACKAGES;
        }

        return noClasses().that()
            .resideInAPackage(base + "..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(forbidden)
            .because(
                "a module is reachable only through its inbound ports; its domain and "
                        + "its adapters are its own business"
            );
    }

    /**
     * Even the allowed cross-module call is confined to the infrastructure layer.
     *
     * <p>
     * If {@code X.application} could name {@code Y.application}, X's use cases
     * would take Y's DTOs as arguments and the two contexts would share a model
     * without anyone deciding to. Confining it to X's infrastructure forces an
     * adapter, and an adapter is where the translation between the two languages
     * belongs.
     */
    public static ArchRule crossModuleCallsGoThroughInfrastructure(String base, String... otherModuleBases) {
        String[] otherApplications = new String[otherModuleBases.length];
        for (int i = 0; i < otherModuleBases.length; i++) {
            otherApplications[i] = otherModuleBases[i] + APPLICATION_PACKAGES;
        }

        return noClasses().that()
            .resideInAnyPackage(base + DOMAIN_PACKAGES, base + APPLICATION_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(otherApplications)
            .because(
                "the inside states what it needs as its own outbound port; "
                        + "the anti-corruption adapter in infrastructure satisfies it"
            );
    }

    // ------------------------------------------------------------------

    public static List<ArchRule> all(String base) {
        return List.of(
            layering(base),
            domainIsFrameworkFree(base),
            applicationIsFrameworkFree(base),
            applicationDoesNotDependOnWeb(base),
            documentsLiveInPersistence(base),
            controllersLiveInWebAdapter(base),
            inboundPortsAreInterfaces(base),
            repositoryPortsAreInterfaces(base),
            adaptersLiveInInfrastructure(base),
            domainDoesNotUseLombok(base)
        );
    }

    public static void check(JavaClasses classes, String base) {
        all(base).forEach(rule -> rule.check(classes));
    }
}
