package com.ead.gearup.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ead.gearup.service.NotificationService;
import com.ead.gearup.service.SseConnectionManager;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final SseConnectionManager sseConnectionManager;

    public NotificationController(NotificationService notificationService, SseConnectionManager sseConnectionManager){
        this.notificationService = notificationService;
        this.sseConnectionManager = sseConnectionManager;
    }

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable String userId) {
        return sseConnectionManager.createConnection(userId);
    }

}
