package com.vertyll.freshly.translation.application.security;

import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;

public enum TranslationPermission implements PermissionDescriptor {
    TRANSLATIONS_READ(Values.TRANSLATIONS_READ),
    TRANSLATIONS_EDIT(Values.TRANSLATIONS_EDIT);

    public static final String CONTEXT = "translation";

    private final String value;

    TranslationPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public PermissionScope scope() {
        return PermissionScope.GLOBAL;
    }

    @Override
    public String context() {
        return CONTEXT;
    }

    public static final class Values {
        public static final String TRANSLATIONS_READ = "translations:read";
        public static final String TRANSLATIONS_EDIT = "translations:edit";

        private Values() {
        }
    }
}
