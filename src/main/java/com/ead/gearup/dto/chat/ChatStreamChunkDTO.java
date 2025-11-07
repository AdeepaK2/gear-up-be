package com.ead.gearup.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for streaming chat chunks (Server-Sent Events)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamChunkDTO {

    /**
     * Partial content chunk
     */
    private String content;

    /**
     * Indicates if this is the final chunk
     */
    @Builder.Default
    private boolean isFinal = false;

    /**
     * Session ID for tracking
     */
    private String sessionId;

    /**
     * Chunk sequence number
     */
    private Integer chunkIndex;
}
