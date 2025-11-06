package com.ead.gearup.event;

// Event fired for project updates
public class ProjectNotificationEvent extends NotificationEvent {
    
    public ProjectNotificationEvent(Object source, String userId, String title, String message) {
        super(source, userId, title, message, "PROJECT_UPDATE");
    }
}
