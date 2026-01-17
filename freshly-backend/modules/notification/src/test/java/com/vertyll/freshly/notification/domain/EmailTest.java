package com.vertyll.freshly.notification.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    private static final String VALID_EMAIL_1 = "user@example.com";
    private static final String VALID_EMAIL_2 = "test.user@domain.com";
    private static final String VALID_EMAIL_3 = "user+tag@example.co.uk";
    private static final String VALID_EMAIL_4 = "user_name@sub-domain.example.com";
    private static final String VALID_EMAIL_5 = "123@example.com";

    private static final String INVALID_EMAIL_1 = "invalid";
    private static final String INVALID_EMAIL_2 = "@example.com";
    private static final String INVALID_EMAIL_3 = "user@";
    private static final String INVALID_EMAIL_4 = "user @example.com";
    private static final String INVALID_EMAIL_5 = "user@.com";
    private static final String INVALID_EMAIL_6 = "user@domain";
    private static final String INVALID_EMAIL_7 = "";
    private static final String INVALID_EMAIL_8 = "user@@example.com";
    private static final String INVALID_EMAIL_9 = "user@exam ple.com";

    private static final String USER1_EMAIL = "user1@example.com";
    private static final String USER2_EMAIL = "user2@example.com";

    private static final String INVALID_EMAIL_FORMAT = "Invalid email format";
    private static final String EMAIL_CANNOT_BE_NULL = "Email cannot be null";

    @ParameterizedTest
    @ValueSource(
            strings = {VALID_EMAIL_1, VALID_EMAIL_2, VALID_EMAIL_3, VALID_EMAIL_4, VALID_EMAIL_5})
    void shouldCreateValidEmail(String emailValue) {
        // When
        Email email = new Email(emailValue);

        // Then
        assertThat(email.value()).isEqualTo(emailValue);
        assertThat(email).hasToString(emailValue);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                INVALID_EMAIL_1,
                INVALID_EMAIL_2,
                INVALID_EMAIL_3,
                INVALID_EMAIL_4,
                INVALID_EMAIL_5,
                INVALID_EMAIL_6,
                INVALID_EMAIL_7,
                INVALID_EMAIL_8,
                INVALID_EMAIL_9
            })
    void shouldThrowException_whenEmailIsInvalid(String invalidEmail) {
        // When & Then
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(INVALID_EMAIL_FORMAT);
    }

    @Test
    @SuppressWarnings("NullAway")
    void shouldThrowException_whenEmailIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(EMAIL_CANNOT_BE_NULL);
    }

    @Test
    void shouldBeEqualForSameEmail() {
        // Given
        Email email1 = new Email(VALID_EMAIL_1);
        Email email2 = new Email(VALID_EMAIL_1);

        // When & Then
        assertThat(email1).isEqualTo(email2).hasSameHashCodeAs(email2);
    }

    @Test
    void shouldNotBeEqualForDifferentEmails() {
        // Given
        Email email1 = new Email(USER1_EMAIL);
        Email email2 = new Email(USER2_EMAIL);

        // When & Then
        assertThat(email1).isNotEqualTo(email2);
    }
}
