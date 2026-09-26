package com.vertyll.freshly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.archunit.PlatformArchitectureRules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

class PlatformArchitectureTest {
    private final JavaClasses classes =
            new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.vertyll.freshly");

    @Test
    @DisplayName("the platform does not depend on any bounded context")
    void platformDoesNotDependOnAnyModule() {
        PlatformArchitectureRules.platformDoesNotDependOnAnyModule().check(classes);
    }

    @Test
    @DisplayName("shared-lang and shared-authz carry nothing but the JDK")
    void frameworkFreePlatformStaysFrameworkFree() {
        PlatformArchitectureRules.frameworkFreePlatformStaysFrameworkFree().check(classes);
    }

    @Test
    @DisplayName("shared-lang holds no domain concept")
    void sharedLanguageHoldsNoDomainConcept() {
        PlatformArchitectureRules.sharedLanguageHoldsNoDomainConcept().check(classes);
    }

    @Test
    @DisplayName("configuration properties belong to the module that reads them")
    void platformHoldsNoModuleConfiguration() {
        PlatformArchitectureRules.platformHoldsNoModuleConfiguration().check(classes);
    }

    @Test
    @DisplayName("the Spring-bound platform is reachable only from adapters")
    void springPlatformIsForAdaptersOnly() {
        PlatformArchitectureRules.springPlatformIsForAdaptersOnly().check(classes);
    }
}
