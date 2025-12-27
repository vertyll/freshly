package com.vertyll.freshly.notification.application.dto;

import com.vertyll.freshly.notification.domain.EmailTemplate;

import java.util.Map;

public record SendEmailCommand(
        String recipientEmail,
        EmailTemplate template,
        Map<String, Object> templateVariables
) {
}
