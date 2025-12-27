package com.vertyll.freshly.notification.domain;

public interface EmailSender {

    void send(EmailNotification notification);
}
