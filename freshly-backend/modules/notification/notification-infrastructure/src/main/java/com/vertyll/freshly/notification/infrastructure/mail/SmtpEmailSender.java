package com.vertyll.freshly.notification.infrastructure.mail;

import java.util.Locale;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.notification.application.port.outbound.EmailDispatchPort;
import com.vertyll.freshly.notification.domain.error.NotificationError;
import com.vertyll.freshly.notification.domain.model.EmailNotification;
import com.vertyll.freshly.notification.infrastructure.config.MailProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
class SmtpEmailSender implements EmailDispatchPort {
    private static final String UTF_8 = "UTF-8";
    private static final String TEMPLATE = "template";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final MessageSource messages;

    @Override
    public void dispatch(EmailNotification notification) {
        Locale locale = Locale.forLanguageTag(notification.languageTag());
        String body = render(notification, locale);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);

            helper.setFrom(mailProperties.from());
            helper.setTo(notification.recipient().value());
            helper.setSubject(subject(notification, locale));
            helper.setText(body, true);

            mailSender.send(message);

            log.debug("Dispatched {} to {}", notification.template(), notification.recipient());

        } catch (MessagingException | MailException e) {
            log.error("SMTP refused {} for {}", notification.template(), notification.recipient(), e);
            throw new DomainException(
                NotificationError.DELIVERY_FAILED,
                Map.of(TEMPLATE, notification.template().name()),
                e
            );
        }
    }

    private String subject(EmailNotification notification, Locale locale) {
        String key = notification.template().subjectKey();
        String resolved = messages.getMessage(key, null, key, locale);
        return resolved == null ? key : resolved;
    }

    private String render(EmailNotification notification, Locale locale) {
        Context context = new Context(locale);
        notification.templateVariables().forEach(context::setVariable);

        try {
            return templateEngine.process(notification.template().templatePath(), context);
        } catch (TemplateProcessingException e) {
            log.error("Cannot render {}", notification.template().templatePath(), e);
            throw new DomainException(
                NotificationError.TEMPLATE_RENDER_FAILED,
                Map.of(TEMPLATE, notification.template().name()),
                e
            );
        }
    }
}
