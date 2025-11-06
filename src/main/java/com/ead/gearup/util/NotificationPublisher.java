package com.ead.gearup.util;

import com.ead.gearup.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// Utility to publish various notification events
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    // Publish an appointment notification
    public void publishAppointmentNotification(String userId, String title, String message) {
        log.debug("Publishing appointment notification for user: {}", userId);
        eventPublisher.publishEvent(new AppointmentNotificationEvent(this, userId, title, message));
    }

    // Publish an appointment notification
    public void publishProjectNotification(String userId, String title, String message) {
        log.debug("Publishing project notification for user: {}", userId);
        eventPublisher.publishEvent(new ProjectNotificationEvent(this, userId, title, message));
    }

    // Publish a task notification
    public void publishTaskNotification(String userId, String title, String message) {
        log.debug("Publishing task notification for user: {}", userId);
        eventPublisher.publishEvent(new TaskNotificationEvent(this, userId, title, message));
    }

    // Publish a system notification
    public void publishSystemNotification(String userId, String title, String message) {
        log.debug("Publishing system notification for user: {}", userId);
        eventPublisher.publishEvent(new SystemNotificationEvent(this, userId, title, message));
    }

    // Publish a custom notification
    public void publishCustomNotification(String userId, String title, String message, String type) {
        log.debug("Publishing custom notification for user: {} with type: {}", userId, type);
        eventPublisher.publishEvent(new NotificationEvent(this, userId, title, message, type) {});
    }
}
