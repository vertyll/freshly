package com.vertyll.freshly.auth;

import com.vertyll.freshly.archunit.FreshlyArchitectureTest;

class AuthArchitectureTest extends FreshlyArchitectureTest {
    AuthArchitectureTest() {
        super("com.vertyll.freshly.auth");
    }

    @Override
    protected String[] neighbouringModules() {
        return new String[] {
            "com.vertyll.freshly.useraccess",
            "com.vertyll.freshly.notification"
        };
    }
}
