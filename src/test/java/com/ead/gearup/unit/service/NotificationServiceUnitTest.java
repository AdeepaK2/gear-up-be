package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ead.gearup.dto.notification.CreateNotificationDTO;
import com.ead.gearup.dto.notification.NotificationDTO;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.model.Notification;
import com.ead.gearup.repository.NotificationRepository;
import com.ead.gearup.service.NotificationService;
import com.ead.gearup.service.SseConnectionManager;

// Unit tests for NotificationService
@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseConnectionManager sseConnectionManager;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private CreateNotificationDTO createNotificationDTO;

    @BeforeEach
    void setUp() {
        // Setup test data
        testNotification = Notification.builder()
                .id(1L)
                .userId("user123")
                .title("Test Notification")
                .message("Test message")
                .type("SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        createNotificationDTO = CreateNotificationDTO.builder()
                .userId("user123")
                .title("Test Notification")
                .message("Test message")
                .type("SYSTEM")
                .build();
    }

    // ========== CREATE NOTIFICATION TESTS ==========

    @Test
    void testCreateAndSendNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(sseConnectionManager).sendToUser(anyString(), any());

        NotificationDTO result = notificationService.createAndSendNotification(createNotificationDTO);

        // Assert: Verify the results
        assertNotNull(result, "Result should not be null");
        assertEquals(testNotification.getId(), result.getId());
        assertEquals(testNotification.getUserId(), result.getUserId());
        assertEquals(testNotification.getTitle(), result.getTitle());
        assertEquals(testNotification.getMessage(), result.getMessage());
        assertEquals(testNotification.getType(), result.getType());
        assertFalse(result.isRead(), "New notification should be unread");

        // Verify that repository save was called exactly once
        verify(notificationRepository, times(1)).save(any(Notification.class));
        
        // Verify that SSE send was called
        verify(sseConnectionManager, times(1)).sendToUser(eq("user123"), any());
    }

    @Test
    void testCreateAndSendNotification_VerifyCorrectDataSaved() {
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notificationCaptor.capture())).thenReturn(testNotification);

        notificationService.createAndSendNotification(createNotificationDTO);

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("user123", savedNotification.getUserId());
        assertEquals("Test Notification", savedNotification.getTitle());
        assertEquals("Test message", savedNotification.getMessage());
        assertEquals("SYSTEM", savedNotification.getType());
        assertFalse(savedNotification.isRead());
    }

    // ========== SEND TO MULTIPLE USERS TESTS ==========

    @Test
    void testSendToMultipleUsers_Success() {
        List<String> userIds = Arrays.asList("user1", "user2", "user3");
        List<Notification> savedNotifications = Arrays.asList(
                createNotificationForUser("user1", 1L),
                createNotificationForUser("user2", 2L),
                createNotificationForUser("user3", 3L)
        );
        
        when(notificationRepository.saveAll(anyList())).thenReturn(savedNotifications);

        notificationService.sendToMultipleUsers(userIds, "Broadcast", "Message for all", "ADMIN");

        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(sseConnectionManager, times(3)).sendToUser(anyString(), any());
    }

    @Test
    void testSendToMultipleUsers_EmptyList() {
        List<String> emptyUserIds = Arrays.asList();
        
        when(notificationRepository.saveAll(anyList())).thenReturn(Arrays.asList());

        notificationService.sendToMultipleUsers(emptyUserIds, "Broadcast", "Message", "ADMIN");

        // Should handle empty list gracefully
        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(sseConnectionManager, never()).sendToUser(anyString(), any());
    }

    // ========== GET NOTIFICATIONS TESTS ==========

    @Test
    void testGetNotifications_WithPagination_Success() {
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications);
        @SuppressWarnings("unused")
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        when(notificationRepository.findByUserId(eq("user123"), any(Pageable.class)))
                .thenReturn(notificationPage);

        Page<NotificationDTO> result = notificationService.getNotifications(
                "user123", 0, 10, "createdAt", "desc", null, null);

        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Notification", result.getContent().get(0).getTitle());
        
        verify(notificationRepository, times(1)).findByUserId(eq("user123"), any(Pageable.class));
    }

    @Test
    void testGetNotifications_WithFilters_Success() {
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications);
        
        when(notificationRepository.findByUserIdWithFilters(
                eq("user123"), eq("SYSTEM"), eq(false), any(Pageable.class)))
                .thenReturn(notificationPage);

        Page<NotificationDTO> result = notificationService.getNotifications(
                "user123", 0, 10, "createdAt", "desc", "SYSTEM", false);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(notificationRepository, times(1)).findByUserIdWithFilters(
                eq("user123"), eq("SYSTEM"), eq(false), any(Pageable.class));
    }

    // ========== UNREAD NOTIFICATIONS TESTS ==========

    @Test
    void testGetUnreadNotifications_Success() {
        List<Notification> unreadNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("user123"))
                .thenReturn(unreadNotifications);

        List<NotificationDTO> result = notificationService.getUnreadNotifications("user123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Notification", result.get(0).getTitle());
        assertFalse(result.get(0).isRead());
        
        verify(notificationRepository, times(1))
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc("user123");
    }

    @Test
    void testGetUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsReadFalse("user123")).thenReturn(5L);

        long count = notificationService.getUnreadCount("user123");

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalse("user123");
    }

    @Test
    void testGetUnreadCount_ZeroUnread() {
        when(notificationRepository.countByUserIdAndIsReadFalse("user123")).thenReturn(0L);

        long count = notificationService.getUnreadCount("user123");

        assertEquals(0L, count);
    }

    // ========== MARK AS READ TESTS ==========

    @Test
    void testMarkAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsRead(1L, "user123");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().isRead(), "Notification should be marked as read");
    }

    @Test
    void testMarkAsRead_NotificationNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> notificationService.markAsRead(999L, "user123"),
                "Should throw ResourceNotFoundException when notification doesn't exist");
        
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsRead_UnauthorizedUser() {
        // Notification belongs to user123, but user456 is trying to mark it
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(IllegalArgumentException.class, 
                () -> notificationService.markAsRead(1L, "user456"),
                "Should throw IllegalArgumentException when user doesn't own the notification");
        
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAllAsRead_Success() {
        Notification notification1 = createNotificationForUser("user123", 1L);
        Notification notification2 = createNotificationForUser("user123", 2L);
        List<Notification> unreadNotifications = Arrays.asList(notification1, notification2);
        
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("user123"))
                .thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(anyList())).thenReturn(unreadNotifications);

        notificationService.markAllAsRead("user123");

        verify(notificationRepository, times(1)).saveAll(anyList());
        
        // Verify all notifications were marked as read
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        captor.getValue().forEach(notification -> 
                assertTrue(notification.isRead(), "All notifications should be marked as read")
        );
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteNotification_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).delete(any(Notification.class));

        notificationService.deleteNotification(1L, "user123");

        verify(notificationRepository, times(1)).delete(testNotification);
    }

    @Test
    void testDeleteNotification_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> notificationService.deleteNotification(999L, "user123"));
        
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void testDeleteNotification_UnauthorizedUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(IllegalArgumentException.class, 
                () -> notificationService.deleteNotification(1L, "user456"));
        
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void testDeleteAllForUser_Success() {
        doNothing().when(notificationRepository).deleteByUserId("user123");

        notificationService.deleteAllForUser("user123");

        verify(notificationRepository, times(1)).deleteByUserId("user123");
    }

    // ========== CLEANUP TESTS ==========

    @Test
    void testCleanupOldNotifications_Success() {
        doNothing().when(notificationRepository).deleteOldReadNotifications(any(LocalDateTime.class));

        notificationService.cleanupOldNotifications(30);

        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository, times(1)).deleteOldReadNotifications(dateCaptor.capture());
        
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expected = LocalDateTime.now().minusDays(30);
        assertTrue(capturedDate.isBefore(LocalDateTime.now()));
        assertTrue(capturedDate.isAfter(expected.minusMinutes(1)));
    }

    // ========== HELPER METHODS ==========

    private Notification createNotificationForUser(String userId, Long id) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .title("Test Notification")
                .message("Test message")
                .type("SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
