package com.vertyll.freshly.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailTemplateTest {

    private static final String USER_REGISTERED_TEMPLATE = "email/user-registered";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "email/email-verification";
    private static final String PASSWORD_RESET_TEMPLATE = "email/password-reset";

    private static final String USER_REGISTERED_SUBJECT = "Welcome to Freshly!";
    private static final String EMAIL_VERIFICATION_SUBJECT = "Verify Your Email Address";
    private static final String PASSWORD_RESET_SUBJECT = "Reset Your Password";

    private static final int EXPECTED_TEMPLATE_COUNT = 3;

    @Test
    void shouldReturnCorrectTemplateNameForUserRegistered() {
        // When
        String templateName = EmailTemplate.USER_REGISTERED.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo(USER_REGISTERED_TEMPLATE);
    }

    @Test
    void shouldReturnCorrectTemplateNameForEmailVerification() {
        // When
        String templateName = EmailTemplate.EMAIL_VERIFICATION.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo(EMAIL_VERIFICATION_TEMPLATE);
    }

    @Test
    void shouldReturnCorrectTemplateNameForPasswordReset() {
        // When
        String templateName = EmailTemplate.PASSWORD_RESET.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo(PASSWORD_RESET_TEMPLATE);
    }

    @Test
    void shouldReturnCorrectSubjectForUserRegistered() {
        // When
        String subject = EmailTemplate.USER_REGISTERED.getSubject();

        // Then
        assertThat(subject).isEqualTo(USER_REGISTERED_SUBJECT);
    }

    @Test
    void shouldReturnCorrectSubjectForEmailVerification() {
        // When
        String subject = EmailTemplate.EMAIL_VERIFICATION.getSubject();

        // Then
        assertThat(subject).isEqualTo(EMAIL_VERIFICATION_SUBJECT);
    }

    @Test
    void shouldReturnCorrectSubjectForPasswordReset() {
        // When
        String subject = EmailTemplate.PASSWORD_RESET.getSubject();

        // Then
        assertThat(subject).isEqualTo(PASSWORD_RESET_SUBJECT);
    }

    @Test
    void shouldHaveThreeTemplates() {
        // When
        EmailTemplate[] templates = EmailTemplate.values();

        // Then
        assertThat(templates)
                .hasSize(EXPECTED_TEMPLATE_COUNT)
                .containsExactlyInAnyOrder(
                        EmailTemplate.USER_REGISTERED,
                        EmailTemplate.EMAIL_VERIFICATION,
                        EmailTemplate.PASSWORD_RESET);
    }
}
