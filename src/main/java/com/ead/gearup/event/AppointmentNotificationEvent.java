package com.ead.gearup.event;

// Event fired for appointment reminders
public class AppointmentNotificationEvent extends NotificationEvent {
    
    public AppointmentNotificationEvent(Object source, String userId, String title, String message) {
        super(source, userId, title, message, "APPOINTMENT");
    }
}
