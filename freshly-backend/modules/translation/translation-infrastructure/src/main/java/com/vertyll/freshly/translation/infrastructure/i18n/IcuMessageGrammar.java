package com.vertyll.freshly.translation.infrastructure.i18n;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;

@Component
public class IcuMessageGrammar implements MessageGrammar {

    @Override
    public Optional<String> rejectionReason(String languageTag, String text) {
        return IcuMessages.validate(languageTag, text);
    }

    @Override
    public Set<String> placeholdersOf(String languageTag, String text) {
        return IcuMessages.argumentsOf(languageTag, text);
    }
}
