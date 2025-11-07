package com.ead.gearup.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for chat responses sent to frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {

    private String answer;

    private String sessionId;

    /**
     * Indicates if response came from cache
     */
    @Builder.Default
    private boolean fromCache = false;

    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;

    /**
     * Timestamp of response
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Confidence score from the LLM (0-1)
     */
    private Double confidence;

    /**
     * Source documents used for RAG context
     */
    private String[] sources;
}
