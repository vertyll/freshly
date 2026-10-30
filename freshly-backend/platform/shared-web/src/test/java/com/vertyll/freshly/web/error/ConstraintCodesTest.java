package com.vertyll.freshly.web.error;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintCodesTest {
    @Test
    void everyWayOfBeingRequiredHasOneKey() {
        assertThat(ConstraintCodes.codeOf("NotNull")).isEqualTo("validation.required");
        assertThat(ConstraintCodes.codeOf("NotBlank")).isEqualTo("validation.required");
        assertThat(ConstraintCodes.codeOf("NotEmpty")).isEqualTo("validation.required");
    }

    @Test
    void aConstraintNobodyMappedFallsBackToTheGenericKey() {
        assertThat(ConstraintCodes.codeOf("Custom")).isEqualTo("validation.invalid");
    }

    @Test
    void onlyTheAttributesATranslationCanUseArePassedOn() {
        Map<String, Object> attributes = Map.of("min", 3, "max", 5, "message", "text", "groups", new Class<?>[0]);

        assertThat(ConstraintCodes.paramsOf(attributes)).containsOnlyKeys("min", "max");
    }
}
