package com.vertyll.freshly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageCoverageTest {
    private static final List<String> LANGUAGES = List.of("en", "pl");

    private final JavaClasses classes =
            new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.vertyll.freshly");

    @Test
    @DisplayName("every DomainError key has a default in every supported language")
    void everyErrorKeyHasDefaults() {
        Map<String, Map<String, String>> declared = declaredDefaults();
        Set<DomainError> catalogue = errorCatalogues();

        assertThat(catalogue).as("no error catalogues were discovered, so this test is checking nothing").isNotEmpty();

        List<String> missing = new ArrayList<>();
        for (DomainError error : catalogue) {
            Map<String, String> texts = declared.get(error.key());
            if (texts == null) {
                missing.add(error.key() + " [no catalogue declares it]");
                continue;
            }
            for (String language : LANGUAGES) {
                if (!texts.containsKey(language) || texts.get(language).isBlank()) {
                    missing.add(error.key() + " [" + language + "]");
                }
            }
        }

        assertThat(missing).as("error keys with no default; add them to the owning module's TranslationCatalogue")
            .isEmpty();
    }

    @Test
    @DisplayName("no two catalogues declare the same key")
    void keysAreDeclaredOnce() {
        List<String> keys = catalogues().stream().flatMap(catalogue -> catalogue.defaults().keySet().stream()).toList();

        List<String> duplicated =
                keys.stream().filter(key -> keys.stream().filter(key::equals).count() > 1).distinct().toList();

        assertThat(duplicated).isEmpty();
    }

    @Test
    @DisplayName("every declared key carries every supported language")
    void catalogueCoverageIsComplete() {
        List<String> gaps = new ArrayList<>();

        declaredDefaults().forEach(
            (key, texts) -> LANGUAGES.stream()
                .filter(language -> !texts.containsKey(language))
                .forEach(language -> gaps.add(key + " [" + language + "]"))
        );

        assertThat(gaps).as("a key shipped in one language and not another renders as the key for half the users")
            .isEmpty();
    }

    @Test
    @DisplayName("every declared default is a compilable ICU pattern")
    void everyDefaultCompiles() {
        List<String> broken = new ArrayList<>();

        catalogues().forEach(
            catalogue -> catalogue.defaults()
                .forEach(
                    (key, texts) -> texts.forEach(
                        (language, text) -> IcuMessages.validate(language, text)
                            .ifPresent(reason -> broken.add(key + " [" + language + "]: " + reason))
                    )
                )
        );

        assertThat(broken)
            .as(
                "an unbalanced brace throws when the message is rendered, which is somewhere "
                        + "far from here and usually on an error path"
            )
            .isEmpty();
    }

    @Test
    @DisplayName("a key uses the same placeholders in every language")
    void placeholdersAgreeAcrossLanguages() {
        List<String> mismatched = declaredDefaults().entrySet()
            .stream()
            .filter(entry -> placeholdersByLanguage(entry.getValue()).values().stream().distinct().count() > 1)
            .map(entry -> entry.getKey() + " " + new TreeMap<>(placeholdersByLanguage(entry.getValue())))
            .toList();

        assertThat(mismatched)
            .as(
                "the call site passes one set of arguments; a language naming a different one "
                        + "renders the placeholder literally, and only for the readers of that language"
            )
            .isEmpty();
    }

    @Test
    @DisplayName("every declared permission has a description in every supported language")
    void everyPermissionIsDescribed() {
        Map<String, Map<String, String>> declared = declaredDefaults();
        List<PermissionDescriptor> permissions =
                permissionCatalogues().stream().flatMap(catalogue -> catalogue.permissions().stream()).toList();

        assertThat(permissions).as("no permission catalogues were discovered, so this test is checking nothing")
            .isNotEmpty();

        List<String> missing = new ArrayList<>();
        permissions.forEach(permission -> {
            Map<String, String> texts = declared.get(permission.descriptionKey());
            if (texts == null) {
                missing.add(permission.value() + " -> " + permission.descriptionKey());
                return;
            }
            LANGUAGES.stream()
                .filter(language -> !texts.containsKey(language) || texts.get(language).isBlank())
                .forEach(language -> missing.add(permission.descriptionKey() + " [" + language + "]"));
        });

        assertThat(missing)
            .as(
                "an administration screen would show these as a bare permission name with no "
                        + "sentence saying what ticking the box does"
            )
            .isEmpty();
    }

    private List<PermissionCatalogue> permissionCatalogues() {
        return classes.stream()
            .filter(candidate -> candidate.isAssignableTo(PermissionCatalogue.class))
            .filter(candidate -> !candidate.isInterface())
            .map(JavaClass::reflect)
            .map(ErrorMessageCoverageTest::instantiatePermissions)
            .toList();
    }

    private static PermissionCatalogue instantiatePermissions(Class<?> type) {
        try {
            return (PermissionCatalogue) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate " + type.getName(), e);
        }
    }

    private Map<String, Map<String, String>> declaredDefaults() {
        return catalogues().stream()
            .flatMap(catalogue -> catalogue.defaults().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second));
    }

    private static Map<String, Set<String>> placeholdersByLanguage(Map<String, String> texts) {
        return texts.entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> IcuMessages.argumentsOf(entry.getKey(), entry.getValue()))
            );
    }

    private List<TranslationCatalogue> catalogues() {
        return classes.stream()
            .filter(candidate -> candidate.isAssignableTo(TranslationCatalogue.class))
            .filter(candidate -> !candidate.isInterface())
            .map(JavaClass::reflect)
            .map(ErrorMessageCoverageTest::instantiate)
            .toList();
    }

    private Set<DomainError> errorCatalogues() {
        return classes.stream()
            .filter(JavaClass::isEnum)
            .filter(candidate -> candidate.isAssignableTo(DomainError.class))
            .map(JavaClass::reflect)
            .flatMap(type -> Arrays.stream(type.getEnumConstants()))
            .map(DomainError.class::cast)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static TranslationCatalogue instantiate(Class<?> type) {
        try {
            return (TranslationCatalogue) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "A TranslationCatalogue must have a no-argument constructor: " + type.getName(),
                e
            );
        }
    }
}
