package com.ead.gearup.event;

// Event fired when a task is assigned to an employee
public class TaskNotificationEvent extends NotificationEvent {
    
    public TaskNotificationEvent(Object source, String userId, String title, String message) {
        super(source, userId, title, message, "TASK_ASSIGNED");
    }
}
