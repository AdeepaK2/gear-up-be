package com.ead.gearup.event;

// Event fired for system-wide notifications
public class SystemNotificationEvent extends NotificationEvent {
    
    public SystemNotificationEvent(Object source, String userId, String title, String message) {
        super(source, userId, title, message, "SYSTEM");
    }
}
