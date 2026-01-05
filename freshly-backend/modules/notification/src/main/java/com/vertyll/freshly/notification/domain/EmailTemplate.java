package com.vertyll.freshly.notification.domain;

import lombok.Getter;

public enum EmailTemplate {
    USER_REGISTERED("user-registered", "Welcome to Freshly!"),
    EMAIL_VERIFICATION("email-verification", "Verify Your Email Address"),
    PASSWORD_RESET("password-reset", "Reset Your Password");

    private final String templateName;
    @Getter
    private final String subject;

    EmailTemplate(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }

    public String getTemplateName() {
        return "email/" + templateName;
    }
}
