package com.ead.gearup.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String question;
    private String sessionId;
    private List<ChatMessage> conversationHistory;
    private Map<String, Object> context;
}