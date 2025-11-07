package com.ead.gearup.event.listener;

import com.ead.gearup.dto.notification.CreateNotificationDTO;
import com.ead.gearup.event.NotificationEvent;
import com.ead.gearup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Notification event listener to handle all notification events
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    // Handle notification events asynchronously
    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Processing notification event: {} for user: {}", 
                event.getClass().getSimpleName(), event.getUserId());
        
        CreateNotificationDTO notificationDTO = CreateNotificationDTO.builder()
                .userId(event.getUserId())
                .title(event.getTitle())
                .message(event.getMessage())
                .type(event.getType())
                .build();
        
        // Use the async method for event-driven notifications
        notificationService.createAndSendNotificationAsync(notificationDTO);
    }
}
