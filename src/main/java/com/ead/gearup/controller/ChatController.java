package com.ead.gearup.controller;

import com.ead.gearup.dto.chat.ChatRequestDTO;
import com.ead.gearup.dto.chat.ChatResponseDTO;
import com.ead.gearup.dto.chat.ChatStreamChunkDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.model.ChatHistory;
import com.ead.gearup.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Controller for AI Chatbot operations
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Chatbot", description = "AI-powered chatbot for appointment queries and customer support")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Send a chat message",
        description = "Send a question to the AI chatbot and receive a response. Uses RAG for context-aware answers about appointments and services."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chat response received successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public Mono<ResponseEntity<ApiResponseDTO<ChatResponseDTO>>> chat(
            @RequestBody @Valid 
            @Parameter(description = "Chat request with question", required = true)
            ChatRequestDTO chatRequest,
            Authentication authentication,
            HttpServletRequest request) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Chat request from user {}: {}", userId, chatRequest.getQuestion());

        return chatService.processChat(chatRequest, userId)
                .map(chatResponse -> {
                    ApiResponseDTO<ChatResponseDTO> response = ApiResponseDTO.<ChatResponseDTO>builder()
                            .status("success")
                            .message("Chat response generated successfully")
                            .data(chatResponse)
                            .timestamp(Instant.now())
                            .path(request.getRequestURI())
                            .build();

                    return ResponseEntity.ok(response);
                });
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Stream chat response",
        description = "Send a question and receive streaming response chunks via Server-Sent Events (SSE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Streaming response",
            content = @Content(mediaType = "text/event-stream")
        )
    })
    public Flux<ServerSentEvent<ChatStreamChunkDTO>> chatStream(
            @RequestBody @Valid 
            @Parameter(description = "Chat request with question", required = true)
            ChatRequestDTO chatRequest,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("Stream chat request from user {}: {}", userId, chatRequest.getQuestion());

        return chatService.processChatStream(chatRequest, userId)
                .map(chunk -> ServerSentEvent.<ChatStreamChunkDTO>builder()
                        .id(chunk.getSessionId() + "-" + chunk.getChunkIndex())
                        .event("chat-chunk")
                        .data(chunk)
                        .build())
                .concatWith(Mono.just(
                    ServerSentEvent.<ChatStreamChunkDTO>builder()
                            .event("end")
                            .build()
                ))
                .delayElements(Duration.ofMillis(10)); // Small delay for smoother streaming
    }

    @GetMapping("/history/{sessionId}")
    @Operation(
        summary = "Get chat history by session",
        description = "Retrieve chat conversation history for a specific session ID"
    )
    public ResponseEntity<ApiResponseDTO<List<ChatHistory>>> getChatHistory(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        List<ChatHistory> history = chatService.getChatHistory(sessionId);

        ApiResponseDTO<List<ChatHistory>> response = ApiResponseDTO.<List<ChatHistory>>builder()
                .status("success")
                .message("Chat history retrieved successfully")
                .data(history)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/user")
    @Operation(
        summary = "Get user's chat history",
        description = "Retrieve all chat conversations for the authenticated user"
    )
    public ResponseEntity<ApiResponseDTO<List<ChatHistory>>> getUserChatHistory(
            Authentication authentication,
            HttpServletRequest request) {

        Long userId = getUserIdFromAuthentication(authentication);
        List<ChatHistory> history = chatService.getUserChatHistory(userId);

        ApiResponseDTO<List<ChatHistory>> response = ApiResponseDTO.<List<ChatHistory>>builder()
                .status("success")
                .message("User chat history retrieved successfully")
                .data(history)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache/clear")
    @Operation(
        summary = "Clear chat cache",
        description = "Clear all cached chat responses (Admin only)"
    )
    public ResponseEntity<ApiResponseDTO<Void>> clearCache(HttpServletRequest request) {
        chatService.clearAllCache();

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Chat cache cleared successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                // Assuming your JWT contains user ID
                // Adjust based on your actual authentication implementation
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                log.warn("Could not parse user ID from authentication: {}", authentication.getName());
            }
        }
        return null;
    }
}
