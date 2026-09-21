package com.vertyll.freshly.notification.application.port.outbound;

import com.vertyll.freshly.notification.domain.model.EmailNotification;

@FunctionalInterface
public interface EmailDispatchPort {
    void dispatch(EmailNotification notification);
}
