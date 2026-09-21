package com.vertyll.freshly.auth.domain.model;

public enum UserRole {
    USER("USER"),
    ADMIN("ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
