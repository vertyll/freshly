package com.vertyll.freshly.archunit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * Base class that runs the shared architecture rules against one bounded context.
 *
 * <p>
 * A module adds a single subclass in its <em>infrastructure</em> test source
 * set, naming its base package:
 *
 * <pre>{@code
 * class UserAccessArchitectureTest extends FreshlyArchitectureTest {
 *     UserAccessArchitectureTest() {
 *         super("com.vertyll.freshly.useraccess");
 *     }
 * }
 * }</pre>
 *
 * <p>
 * It lives in the infrastructure module on purpose: that is the only project
 * whose test classpath carries all three layers, so importing the base package
 * there sees the whole context. Putting it in the domain module would leave two
 * of the three layers empty and every rule would pass for the wrong reason.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class FreshlyArchitectureTest {

    private final String basePackage;
    private final JavaClasses classes;

    protected FreshlyArchitectureTest(String basePackage) {
        this.basePackage = basePackage;
        this.classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(basePackage);
    }

    @Test
    @DisplayName("the dependency rule points inwards")
    void dependencyRulePointsInwards() {
        FreshlyArchitectureRules.layering(basePackage).check(classes);
    }

    @Test
    @DisplayName("the domain layer is framework-free")
    void domainLayerIsFrameworkFree() {
        FreshlyArchitectureRules.domainIsFrameworkFree(basePackage).check(classes);
    }

    @Test
    @DisplayName("the application layer is framework-free")
    void applicationLayerIsFrameworkFree() {
        FreshlyArchitectureRules.applicationIsFrameworkFree(basePackage).check(classes);
    }

    @Test
    @DisplayName("the inside does not depend on the web adapter")
    void applicationDoesNotDependOnWeb() {
        FreshlyArchitectureRules.applicationDoesNotDependOnWeb(basePackage).check(classes);
    }

    @Test
    @DisplayName("MongoDB documents live in the persistence adapter")
    void documentsLiveInPersistence() {
        FreshlyArchitectureRules.documentsLiveInPersistence(basePackage).check(classes);
    }

    @Test
    @DisplayName("controllers live in the web adapter")
    void controllersLiveInWebAdapter() {
        FreshlyArchitectureRules.controllersLiveInWebAdapter(basePackage).check(classes);
    }

    @Test
    @DisplayName("inbound ports are interfaces")
    void inboundPortsAreInterfaces() {
        FreshlyArchitectureRules.inboundPortsAreInterfaces(basePackage).check(classes);
    }

    @Test
    @DisplayName("repository ports are interfaces")
    void repositoryPortsAreInterfaces() {
        FreshlyArchitectureRules.repositoryPortsAreInterfaces(basePackage).check(classes);
    }

    @Test
    @DisplayName("adapters live in infrastructure")
    void adaptersLiveInInfrastructure() {
        FreshlyArchitectureRules.adaptersLiveInInfrastructure(basePackage).check(classes);
    }

    @Test
    @DisplayName("the domain does not use Lombok to generate its shape")
    void domainDoesNotUseLombok() {
        FreshlyArchitectureRules.domainDoesNotUseLombok(basePackage).check(classes);
    }

    // --- ArchUnit's own general coding rules ---

    @Test
    @DisplayName("nothing writes to standard streams")
    void nothingWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes);
    }

    @Test
    @DisplayName("nothing throws generic exceptions")
    void nothingThrowsGenericExceptions() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes);
    }

    @Test
    @DisplayName("nothing uses java.util.logging")
    void nothingUsesJavaUtilLogging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes);
    }

    @Test
    @DisplayName("nothing uses field injection")
    void nothingUsesFieldInjection() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes);
    }

    /**
     * Overridden by a module that has neighbours, naming their base packages.
     *
     * <p>
     * Default is empty rather than abstract so that adding a module does not
     * break every existing test class — but a context with no override is
     * announcing that it talks to nobody, which is worth noticing in review.
     */
    protected String[] neighbouringModules() {
        return new String[0];
    }

    @Test
    @DisplayName("this module does not reach into another module's internals")
    void doesNotReachIntoOtherModules() {
        String[] neighbours = neighbouringModules();
        if (neighbours.length == 0) {
            return;
        }
        FreshlyArchitectureRules.modulesDoNotReachIntoEachOthersInternals(basePackage, neighbours)
            .check(
                new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(concat(basePackage, neighbours))
            );
    }

    @Test
    @DisplayName("cross-module calls go through an infrastructure adapter")
    void crossModuleCallsGoThroughInfrastructure() {
        String[] neighbours = neighbouringModules();
        if (neighbours.length == 0) {
            return;
        }
        FreshlyArchitectureRules.crossModuleCallsGoThroughInfrastructure(basePackage, neighbours)
            .check(
                new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(concat(basePackage, neighbours))
            );
    }

    private static String[] concat(String first, String... rest) {
        String[] all = new String[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
}
