package com.vertyll.freshly.auth.domain.model;

public enum TokenPurpose {
    EMAIL_VERIFICATION("email_verification"),
    PASSWORD_RESET("password_reset");

    private final String claimValue;

    TokenPurpose(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return claimValue;
    }
}
