package com.vertyll.freshly.notification.domain.model;

public enum EmailTemplate {
    USER_REGISTERED("user-registered", "email.userRegistered.title"),
    EMAIL_VERIFICATION("email-verification", "email.verification.title"),
    PASSWORD_RESET("password-reset", "email.passwordReset.title");

    private static final String TEMPLATE_PREFIX = "email/";

    private final String templateName;
    private final String subjectKey;

    EmailTemplate(String templateName, String subjectKey) {
        this.templateName = templateName;
        this.subjectKey = subjectKey;
    }

    public String templatePath() {
        return TEMPLATE_PREFIX + templateName;
    }

    public String subjectKey() {
        return subjectKey;
    }
}
