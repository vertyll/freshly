package com.vertyll.freshly.airquality.application.security;

import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;

public enum AirQualityPermission implements PermissionDescriptor {
    AIRQUALITY_SYNC(Values.AIRQUALITY_SYNC),
    AIRQUALITY_PURGE(Values.AIRQUALITY_PURGE);

    public static final String CONTEXT_NAME = "airquality";

    private final String value;

    AirQualityPermission(String value) {
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
        return CONTEXT_NAME;
    }

    public static final class Values {
        public static final String AIRQUALITY_SYNC = "airquality:sync";
        public static final String AIRQUALITY_PURGE = "airquality:purge";

        private Values() {
        }
    }
}
