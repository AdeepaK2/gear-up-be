package com.ead.gearup.controller;

import com.ead.gearup.dto.notification.CreateNotificationDTO;
import com.ead.gearup.dto.notification.NotificationDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ead.gearup.service.NotificationService;
import com.ead.gearup.service.SseConnectionManager;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Real-time notification management via SSE")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final SseConnectionManager sseConnectionManager;

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream notifications via SSE", 
               description = "Establishes a Server-Sent Events connection for real-time notifications")
    public SseEmitter streamNotifications(@PathVariable String userId) {
        return sseConnectionManager.createConnection(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get notification history", 
               description = "Retrieve paginated notifications with optional filters")
    public ResponseEntity<ApiResponseDTO<Page<NotificationDTO>>> getNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead) {

        Page<NotificationDTO> notifications = notificationService.getNotifications(
                userId, page, size, sort, direction, type, isRead);
        
        return ResponseEntity.ok(ApiResponseDTO.<Page<NotificationDTO>>builder()
                .status("success")
                .message("Notifications retrieved successfully")
                .data(notifications)
                .build());
    }

    @GetMapping("/{userId}/unread")
    @Operation(summary = "Get unread notifications", 
               description = "Retrieve all unread notifications for a user")
    public ResponseEntity<ApiResponseDTO<List<NotificationDTO>>> getUnreadNotifications(
            @PathVariable String userId) {
        
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<List<NotificationDTO>>builder()
                .status("success")
                .message("Unread notifications retrieved successfully")
                .data(notifications)
                .build());
    }

    @GetMapping("/{userId}/unread/count")
    @Operation(summary = "Get unread count", 
               description = "Get the count of unread notifications")
    public ResponseEntity<ApiResponseDTO<Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Long>builder()
                .status("success")
                .message("Unread count retrieved successfully")
                .data(count)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create notification", 
               description = "Create and send a notification to a user")
    public ResponseEntity<ApiResponseDTO<NotificationDTO>> createNotification(
            @Valid @RequestBody CreateNotificationDTO createNotificationDTO) {
        
        NotificationDTO notification = notificationService.createAndSendNotification(createNotificationDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.<NotificationDTO>builder()
                        .status("success")
                        .message("Notification created and sent successfully")
                        .data(notification)
                        .build());
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", 
               description = "Mark a specific notification as read")
    public ResponseEntity<ApiResponseDTO<Void>> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        
        notificationService.markAsRead(notificationId, userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Notification marked as read")
                .build());
    }

    @PatchMapping("/{userId}/read-all")
    @Operation(summary = "Mark all as read", 
               description = "Mark all notifications as read for a user")
    public ResponseEntity<ApiResponseDTO<Void>> markAllAsRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status("success")
                .message("All notifications marked as read")
                .build());
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", 
               description = "Delete a specific notification")
    public ResponseEntity<ApiResponseDTO<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        
        notificationService.deleteNotification(notificationId, userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Notification deleted successfully")
                .build());
    }

    @DeleteMapping("/{userId}/all")
    @Operation(summary = "Delete all notifications", 
               description = "Delete all notifications for a user")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAllNotifications(@PathVariable String userId) {
        notificationService.deleteAllForUser(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status("success")
                .message("All notifications deleted successfully")
                .build());
    }

    @GetMapping("/stats/{userId}")
    @Operation(summary = "Get connection stats", 
               description = "Get SSE connection statistics for a user")
    public ResponseEntity<ApiResponseDTO<Integer>> getConnectionStats(@PathVariable String userId) {
        int connectionCount = sseConnectionManager.getConnectionCount(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.<Integer>builder()
                .status("success")
                .message("Connection count retrieved")
                .data(connectionCount)
                .build());
    }
}
