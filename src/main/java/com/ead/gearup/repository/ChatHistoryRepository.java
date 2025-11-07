package com.ead.gearup.repository;

import com.ead.gearup.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ChatHistory entity
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /**
     * Find chat history by session ID
     */
    List<ChatHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find chat history by user ID
     */
    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find recent conversations
     */
    List<ChatHistory> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    /**
     * Count total chats by user
     */
    Long countByUserId(Long userId);
}
