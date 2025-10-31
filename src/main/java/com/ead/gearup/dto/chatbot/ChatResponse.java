package com.ead.gearup.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String sessionId;
    private LocalDateTime timestamp;
    private Double confidence;
    private List<String> sources;
    private Boolean fromCache;
    private Integer processingTimeMs;
}