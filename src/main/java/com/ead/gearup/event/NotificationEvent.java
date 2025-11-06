package com.ead.gearup.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class NotificationEvent extends ApplicationEvent {
    
    private final String userId;
    private final String title;
    private final String message;
    private final String type;

    public NotificationEvent(Object source, String userId, String title, String message, String type) {
        super(source);
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
    }
}
