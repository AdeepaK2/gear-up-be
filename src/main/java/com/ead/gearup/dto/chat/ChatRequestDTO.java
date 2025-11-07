package com.ead.gearup.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for incoming chat requests from frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 1000, message = "Question cannot exceed 1000 characters")
    private String question;

    /**
     * Optional: Session ID for maintaining conversation context
     */
    private String sessionId;

    /**
     * Optional: Previous conversation history for context
     */
    private List<ChatMessageDTO> conversationHistory;

    /**
     * Optional: Specific appointment date to query
     */
    private String appointmentDate;

    /**
     * Optional: Service type filter
     */
    private String serviceType;
}
