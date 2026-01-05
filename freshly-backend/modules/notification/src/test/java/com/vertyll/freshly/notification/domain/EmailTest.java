package com.vertyll.freshly.notification.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "user@example.com",
                "test.user@domain.com",
                "user+tag@example.co.uk",
                "user_name@sub-domain.example.com",
                "123@example.com"
            })
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
                "invalid",
                "@example.com",
                "user@",
                "user @example.com",
                "user@.com",
                "user@domain",
                "",
                "user@@example.com",
                "user@exam ple.com"
            })
    void shouldThrowException_whenEmailIsInvalid(String invalidEmail) {
        // When & Then
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldThrowException_whenEmailIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email cannot be null");
    }

    @Test
    void shouldBeEqualForSameEmail() {
        // Given
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");

        // When & Then
        assertThat(email1).isEqualTo(email2).hasSameHashCodeAs(email2);
    }

    @Test
    void shouldNotBeEqualForDifferentEmails() {
        // Given
        Email email1 = new Email("user1@example.com");
        Email email2 = new Email("user2@example.com");

        // When & Then
        assertThat(email1).isNotEqualTo(email2);
    }
}
