package com.ead.gearup.repository;

import com.ead.gearup.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all notifications for a specific user ordered by creation date descending
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find all notifications for a specific user with pagination
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Find unread notifications for a user
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    // Find notifications by user and type
    Page<Notification> findByUserIdAndType(String userId, String type, Pageable pageable);

    // Find notifications by user and read status
    Page<Notification> findByUserIdAndIsRead(String userId, boolean isRead, Pageable pageable);

    // Count unread notifications for a user
    long countByUserIdAndIsReadFalse(String userId);

    // Find notifications with optional filters for type and read status
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:isRead IS NULL OR n.isRead = :isRead)")
    Page<Notification> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("isRead") Boolean isRead,
            Pageable pageable
    );

    // Delete all notifications for a user
    void deleteByUserId(String userId);

    // Delete read notifications older than a certain date
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
