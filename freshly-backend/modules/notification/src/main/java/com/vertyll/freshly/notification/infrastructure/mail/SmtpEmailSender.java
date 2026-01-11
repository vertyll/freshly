package com.vertyll.freshly.notification.infrastructure.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.notification.domain.EmailNotification;
import com.vertyll.freshly.notification.domain.EmailSender;
import com.vertyll.freshly.notification.domain.exception.EmailSendingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    @Override
    public void send(EmailNotification notification) {
        try {
            String htmlContent = renderTemplate(notification);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.from());
            helper.setTo(notification.getRecipient().value());
            helper.setSubject(notification.getTemplate().getSubject());
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.debug(
                    "Email sent successfully: template={}, recipient={}",
                    notification.getTemplate(),
                    notification.getRecipient());

        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    private String renderTemplate(EmailNotification notification) {
        Context context = new Context();
        notification.getTemplateVariables().forEach(context::setVariable);

        return templateEngine.process(notification.getTemplate().getTemplateName(), context);
    }
}
