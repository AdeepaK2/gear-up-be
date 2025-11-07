package com.ead.gearup.service;

import com.ead.gearup.dto.notification.NotificationEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Service to manage SSE connections and send notifications
@Slf4j
@Service
public class SseConnectionManager {

    // Timeout for SSE connections (30 minutes)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    // Store multiple emitters per user (for multiple browser tabs/devices)
    private final ConcurrentHashMap<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // Create a new SSE connection for a user
    public SseEmitter createConnection(String userId) {
        log.info("Creating SSE connection for user: {}", userId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Add emitter to user's connection list
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        log.info("User {} now has {} active connection(s)", userId, userEmitters.get(userId).size());
        
        // Handle completion (client closes connection normally)
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            removeEmitter(userId, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for user: {}", userId);
            removeEmitter(userId, emitter);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for user: {}", userId, ex);
            removeEmitter(userId, emitter);
        });
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification stream"));
        } catch (IOException e) {
            log.error("Error sending initial connection message to user: {}", userId, e);
            removeEmitter(userId, emitter);
        }
        
        return emitter;
    }

    // Send a notification to a specific user
    public void sendToUser(String userId, NotificationEventDTO notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active connections for user: {}", userId);
            return;
        }
        
        log.info("Sending notification to user {} ({} connection(s))", userId, emitters.size());
        
        // Send to all active connections for this user
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.error("Failed to send notification to user: {}", userId, e);
                deadEmitters.add(emitter);
            }
        }
        
        // Remove dead emitters
        deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
    }

    // Send a notification to multiple users
    public void sendToUsers(List<String> userIds, NotificationEventDTO notification) {
        userIds.forEach(userId -> sendToUser(userId, notification));
    }

    // Broadcast a notification to all connected users
    public void broadcastToAll(NotificationEventDTO notification) {
        log.info("Broadcasting notification to all users ({} users connected)", userEmitters.size());
        userEmitters.keySet().forEach(userId -> sendToUser(userId, notification));
    }

    // Remove a specific emitter for a user
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        
        if (emitters != null) {
            emitters.remove(emitter);
            log.info("Removed emitter for user {}. Remaining connections: {}", userId, emitters.size());
            
            // If no more emitters, remove the user entry
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
                log.info("User {} has no more active connections", userId);
            }
        }
        
        emitter.complete();
    }

    // Get the number of active connections for a specific user
    public int getConnectionCount(String userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters == null ? 0 : emitters.size();
    }

    // Get the total number of connected users
    public int getTotalConnectedUsers() {
        return userEmitters.size();
    }

    // Disconnect a specific user (close all their connections)
    public void disconnectUser(String userId) {
        List<SseEmitter> emitters = userEmitters.remove(userId);
        
        if (emitters != null) {
            log.info("Disconnecting all {} connection(s) for user: {}", emitters.size(), userId);
            emitters.forEach(SseEmitter::complete);
        }
    }

    // Disconnect all users
    public void disconnectAll() {
        log.info("Disconnecting all users ({} users)", userEmitters.size());
        userEmitters.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        userEmitters.clear();
    }
}
