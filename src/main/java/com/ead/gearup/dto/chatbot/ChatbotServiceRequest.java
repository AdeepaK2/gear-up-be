package com.ead.gearup.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotServiceRequest {
    private String question;
    private String sessionId;
    private List<ChatMessage> conversationHistory;
    private String appointmentDate;
    private String serviceType;
    private Long customerId;
    private Long userId;  // User ID for session filtering
    private String customerEmail;
    private String authToken;
}