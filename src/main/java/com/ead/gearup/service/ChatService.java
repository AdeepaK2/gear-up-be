package com.ead.gearup.service;

import com.ead.gearup.dto.chat.ChatRequestDTO;
import com.ead.gearup.dto.chat.ChatResponseDTO;
import com.ead.gearup.dto.chat.ChatStreamChunkDTO;
import com.ead.gearup.model.ChatHistory;
import com.ead.gearup.repository.ChatHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling chat operations with RAG chatbot
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final WebClient chatbotWebClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatHistoryRepository chatHistoryRepository;

    private static final String CACHE_PREFIX = "chat:response:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * Process chat request with caching
     */
    public Mono<ChatResponseDTO> processChat(ChatRequestDTO request, Long userId) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        // Generate cache key from question
        String cacheKey = generateCacheKey(request.getQuestion());

        // Check cache first
        ChatResponseDTO cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            log.info("Cache hit for question: {}", request.getQuestion());
            cachedResponse.setFromCache(true);
            cachedResponse.setSessionId(sessionId);
            
            // Save to history
            saveChatHistory(request, cachedResponse, userId);
            return Mono.just(cachedResponse);
        }

        // Call Python service with userId in header
        return chatbotWebClient.post()
                .uri("/chat")
                .header("X-User-Id", userId.toString())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonResponse -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    ChatResponseDTO response = ChatResponseDTO.builder()
                            .answer(jsonResponse.get("answer").asText())
                            .sessionId(sessionId)
                            .fromCache(false)
                            .processingTimeMs(processingTime)
                            .timestamp(Instant.now())
                            .confidence(jsonResponse.has("confidence") ? jsonResponse.get("confidence").asDouble() : null)
                            .sources(extractSources(jsonResponse))
                            .build();

                    // Cache the response
                    cacheResponse(cacheKey, response);
                    
                    // Save to history
                    saveChatHistory(request, response, userId);

                    return response;
                })
                .doOnError(error -> log.error("Error calling Python chatbot service: {}", error.getMessage()))
                .onErrorResume(error -> Mono.just(
                    ChatResponseDTO.builder()
                            .answer("I'm sorry, I'm having trouble processing your request right now. Please try again later.")
                            .sessionId(sessionId)
                            .fromCache(false)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .timestamp(Instant.now())
                            .build()
                ));
    }

    /**
     * Process chat with streaming response (SSE)
     */
    public Flux<ChatStreamChunkDTO> processChatStream(ChatRequestDTO request, Long userId) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String cacheKey = generateCacheKey(request.getQuestion());

        // Check cache first
        ChatResponseDTO cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            log.info("Cache hit for streaming question: {}", request.getQuestion());
            
            // Save to history
            saveChatHistory(request, cachedResponse, userId);
            
            // Return as single chunk
            return Flux.just(ChatStreamChunkDTO.builder()
                    .content(cachedResponse.getAnswer())
                    .isFinal(true)
                    .sessionId(sessionId)
                    .chunkIndex(0)
                    .build());
        }

        // Stream from Python service with userId in header
        return chatbotWebClient.post()
                .uri("/chat/stream")
                .header("X-User-Id", userId.toString())
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(jsonChunk -> ChatStreamChunkDTO.builder()
                        .content(jsonChunk.get("content").asText())
                        .isFinal(jsonChunk.has("is_final") && jsonChunk.get("is_final").asBoolean())
                        .sessionId(sessionId)
                        .chunkIndex(jsonChunk.has("chunk_index") ? jsonChunk.get("chunk_index").asInt() : 0)
                        .build())
                .doOnError(error -> log.error("Error streaming from Python chatbot service: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just(
                    ChatStreamChunkDTO.builder()
                            .content("I'm sorry, I'm having trouble processing your request right now.")
                            .isFinal(true)
                            .sessionId(sessionId)
                            .chunkIndex(0)
                            .build()
                ));
    }

    /**
     * Get chat history for a session
     */
    public List<ChatHistory> getChatHistory(String sessionId) {
        return chatHistoryRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    /**
     * Get chat history for a user
     */
    public List<ChatHistory> getUserChatHistory(Long userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Clear cache for a specific question
     */
    public void clearCache(String question) {
        String cacheKey = generateCacheKey(question);
        redisTemplate.delete(cacheKey);
        log.info("Cleared cache for question: {}", question);
    }

    /**
     * Clear all chat cache
     */
    public void clearAllCache() {
        redisTemplate.keys(CACHE_PREFIX + "*").forEach(redisTemplate::delete);
        log.info("Cleared all chat cache");
    }

    // Private helper methods

    private String generateCacheKey(String question) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(question.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            return CACHE_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating cache key", e);
            return CACHE_PREFIX + question.hashCode();
        }
    }

    private ChatResponseDTO getCachedResponse(String cacheKey) {
        try {
            return (ChatResponseDTO) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("Error retrieving from cache", e);
            return null;
        }
    }

    private void cacheResponse(String cacheKey, ChatResponseDTO response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
            log.debug("Cached response for key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Error caching response", e);
        }
    }

    private void saveChatHistory(ChatRequestDTO request, ChatResponseDTO response, Long userId) {
        try {
            ChatHistory history = ChatHistory.builder()
                    .sessionId(response.getSessionId())
                    .question(request.getQuestion())
                    .answer(response.getAnswer())
                    .userId(userId)
                    .fromCache(response.isFromCache())
                    .processingTimeMs(response.getProcessingTimeMs())
                    .confidenceScore(response.getConfidence())
                    .build();

            chatHistoryRepository.save(history);
            log.debug("Saved chat history for session: {}", response.getSessionId());
        } catch (Exception e) {
            log.error("Error saving chat history", e);
        }
    }

    private String[] extractSources(JsonNode jsonResponse) {
        if (jsonResponse.has("sources") && jsonResponse.get("sources").isArray()) {
            return jsonResponse.get("sources").findValuesAsText("source")
                    .toArray(new String[0]);
        }
        return new String[0];
    }
}
