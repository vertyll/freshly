package com.vertyll.freshly.notification.application.command;

import java.util.Map;

import com.vertyll.freshly.notification.domain.model.EmailTemplate;

public record SendEmailCommand(
    String recipientEmail,
    EmailTemplate template,
    Map<String, Object> templateVariables,
    String languageTag
) {
    public SendEmailCommand {
        templateVariables = Map.copyOf(templateVariables);
    }
}
