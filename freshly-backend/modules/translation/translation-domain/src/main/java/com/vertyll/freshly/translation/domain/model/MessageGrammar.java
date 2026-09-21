package com.vertyll.freshly.translation.domain.model;

import java.util.Optional;
import java.util.Set;

public interface MessageGrammar {

    Optional<String> rejectionReason(String languageTag, String text);

    Set<String> placeholdersOf(String languageTag, String text);
}
