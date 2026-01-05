package com.vertyll.freshly.notification.application.dto;

import java.util.Map;

import com.vertyll.freshly.notification.domain.EmailTemplate;

public record SendEmailCommand(
        String recipientEmail, EmailTemplate template, Map<String, Object> templateVariables) {}
