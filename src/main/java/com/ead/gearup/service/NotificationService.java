package com.ead.gearup.service;

import com.ead.gearup.dto.notification.CreateNotificationDTO;
import com.ead.gearup.dto.notification.NotificationDTO;
import com.ead.gearup.dto.notification.NotificationEventDTO;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.model.Notification;
import com.ead.gearup.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseConnectionManager sseConnectionManager;

    /**
     * Create and send a notification synchronously (used by REST API)
     * 
     * @param createNotificationDTO The notification data
     * @return The created notification
     */
    @Transactional
    public NotificationDTO createAndSendNotification(CreateNotificationDTO createNotificationDTO) {
        log.info("Creating notification for user: {}", createNotificationDTO.getUserId());
        
        // Save to database
        Notification notification = Notification.builder()
                .userId(createNotificationDTO.getUserId())
                .title(createNotificationDTO.getTitle())
                .message(createNotificationDTO.getMessage())
                .type(createNotificationDTO.getType())
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved with ID: {}", savedNotification.getId());
        
        // Send via SSE if user is connected
        NotificationEventDTO eventDTO = convertToEventDTO(savedNotification);
        sseConnectionManager.sendToUser(savedNotification.getUserId(), eventDTO);
        
        return convertToDTO(savedNotification);
    }

    // Create and send a notification asynchronously (used by internal services)
    @Async
    @Transactional
    public void createAndSendNotificationAsync(CreateNotificationDTO createNotificationDTO) {
        log.info("Creating notification asynchronously for user: {}", createNotificationDTO.getUserId());
        
        // Save to database
        Notification notification = Notification.builder()
                .userId(createNotificationDTO.getUserId())
                .title(createNotificationDTO.getTitle())
                .message(createNotificationDTO.getMessage())
                .type(createNotificationDTO.getType())
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved with ID: {}", savedNotification.getId());
        
        // Send via SSE if user is connected
        NotificationEventDTO eventDTO = convertToEventDTO(savedNotification);
        sseConnectionManager.sendToUser(savedNotification.getUserId(), eventDTO);
    }

    // Send notification to multiple users
    @Async
    @Transactional
    public void sendToMultipleUsers(List<String> userIds, String title, String message, String type) {
        log.info("Sending notification to {} users", userIds.size());
        
        List<Notification> notifications = userIds.stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type(type)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());
        
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        
        // Send via SSE
        savedNotifications.forEach(notification -> {
            NotificationEventDTO eventDTO = convertToEventDTO(notification);
            sseConnectionManager.sendToUser(notification.getUserId(), eventDTO);
        });
    }

    // Get paginated notifications with optional filters
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(
            String userId, 
            int page, 
            int size, 
            String sort, 
            String direction,
            String type,
            Boolean isRead) {
        
        log.info("Fetching notifications for user: {} (page: {}, size: {})", userId, page, size);
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Notification> notificationPage;
        
        if (type != null || isRead != null) {
            notificationPage = notificationRepository.findByUserIdWithFilters(userId, type, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByUserId(userId, pageable);
        }
        
        return notificationPage.map(this::convertToDTO);
    }

    // Get unread notifications for a user
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get count of unread notifications
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // Mark a notification as read
    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        log.info("Marking notification {} as read for user: {}", notificationId, userId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        
        // Validate that the notification belongs to the user
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // Mark all notifications as read for a user
    @Transactional
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        
        List<Notification> unreadNotifications = 
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    // Delete a specific notification
    @Transactional
    public void deleteNotification(Long notificationId, String userId) {
        log.info("Deleting notification {} for user: {}", notificationId, userId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        
        // Validate that the notification belongs to the user
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }
        
        notificationRepository.delete(notification);
    }

    // Delete all notifications for a user
    @Transactional
    public void deleteAllForUser(String userId) {
        log.info("Deleting all notifications for user: {}", userId);
        notificationRepository.deleteByUserId(userId);
    }

    // Cleanup old read notifications
    @Transactional
    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        log.info("Cleaning up read notifications older than: {}", cutoffDate);
        notificationRepository.deleteOldReadNotifications(cutoffDate);
    }

    // Convert Notification entity to NotificationDTO
    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // Convert Notification entity to NotificationEventDTO
    private NotificationEventDTO convertToEventDTO(Notification notification) {
        return NotificationEventDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .timestamp(notification.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
