package com.ead.gearup.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual chat messages in conversation history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    /**
     * Role: 'user' or 'assistant'
     */
    private String role;

    /**
     * Message content
     */
    private String content;

    /**
     * Timestamp
     */
    private String timestamp;
}
