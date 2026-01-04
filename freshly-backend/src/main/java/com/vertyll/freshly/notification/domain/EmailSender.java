package com.vertyll.freshly.notification.domain;

@FunctionalInterface
public interface EmailSender {

    void send(EmailNotification notification);
}
