package com.vertyll.freshly.translation.application.port.inbound.command;

import java.util.Map;
import java.util.Set;

public interface TranslationRegistrationUseCase {
    int registerDefaults(String context, Map<String, Map<String, String>> defaults);

    int markOrphans(Set<String> declaredKeys);
}
