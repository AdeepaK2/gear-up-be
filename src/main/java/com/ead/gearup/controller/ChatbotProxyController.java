package com.ead.gearup.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ead.gearup.dto.chatbot.ChatRequest;
import com.ead.gearup.dto.chatbot.ChatResponse;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/rag-chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RAG Chat Proxy", description = "Proxy endpoints for RAG chatbot integration")
@Slf4j
public class ChatbotProxyController {

    private final WebClient.Builder webClientBuilder;
    private final CustomerService customerService;

    @Value("${chatbot.service.url:http://localhost:8000}")
    private String chatbotServiceUrl;

    /**
     * Process chat request through RAG service
     */
    @PostMapping
    @Operation(
        summary = "Send chat message",
        description = "Process chat message through RAG chatbot service with customer context"
    )
    public ResponseEntity<ApiResponseDTO<ChatResponse>> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        try {
            log.info("Processing chat request: {}", request.getQuestion().substring(0, Math.min(50, request.getQuestion().length())));

            // Get authenticated customer context
            String customerEmail = getCurrentCustomerEmail();
            Long customerId = customerService.getCustomerIdByEmail(customerEmail);

            // Prepare request for Python chatbot service
            var chatbotRequest = com.ead.gearup.dto.chatbot.ChatbotServiceRequest.builder()
                    .question(request.getQuestion())
                    .sessionId(request.getSessionId())
                    .conversationHistory(request.getConversationHistory())
                    .customerId(customerId)
                    .customerEmail(customerEmail)
                    .authToken(getCurrentAuthToken())
                    .build();

            // Call Python chatbot service
            WebClient webClient = webClientBuilder.build();
            ChatResponse chatbotResponse = webClient
                    .post()
                    .uri(chatbotServiceUrl + "/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatbotRequest)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            ApiResponseDTO<ChatResponse> response = ApiResponseDTO.<ChatResponse>builder()
                    .status("success")
                    .message("Chat response generated successfully")
                    .data(chatbotResponse)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            log.error("Error calling chatbot service: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.<ChatResponse>builder()
                            .status("error")
                            .message("Chatbot service error: " + e.getResponseBodyAsString())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<ChatResponse>builder()
                            .status("error")
                            .message("Internal server error: " + e.getMessage())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        }
    }

    /**
     * Stream chat response through RAG service
     */
    @PostMapping("/stream")
    @Operation(
        summary = "Stream chat message",
        description = "Process chat message with streaming response through RAG chatbot service"
    )
    public ResponseEntity<String> streamChat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        try {
            log.info("Processing stream chat request: {}", request.getQuestion().substring(0, Math.min(50, request.getQuestion().length())));

            // For now, redirect to Python service directly
            // TODO: Implement proper streaming proxy that includes authentication context
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .header("Location", chatbotServiceUrl + "/chat/stream")
                    .body("Redirecting to streaming endpoint");

        } catch (Exception e) {
            log.error("Error processing stream chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get chat history for current customer
     */
    @GetMapping("/history/{sessionId}")
    @Operation(
        summary = "Get chat history",
        description = "Retrieve chat history for a session"
    )
    public ResponseEntity<ApiResponseDTO<Object>> getChatHistory(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        try {
            log.info("Getting chat history for session: {}", sessionId);

            WebClient webClient = webClientBuilder.build();
            Object history = webClient
                    .get()
                    .uri(chatbotServiceUrl + "/chat/history/" + sessionId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            ApiResponseDTO<Object> response = ApiResponseDTO.<Object>builder()
                    .status("success")
                    .message("Chat history retrieved successfully")
                    .data(history)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Error getting chat history: " + e.getMessage())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        }
    }

    /**
     * Health check for chatbot service
     */
    @GetMapping("/health")
    @Operation(
        summary = "Chatbot service health check",
        description = "Check the health status of the chatbot service"
    )
    public ResponseEntity<ApiResponseDTO<Object>> healthCheck(HttpServletRequest httpRequest) {
        try {
            WebClient webClient = webClientBuilder.build();
            Object health = webClient
                    .get()
                    .uri(chatbotServiceUrl + "/health")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            ApiResponseDTO<Object> response = ApiResponseDTO.<Object>builder()
                    .status("success")
                    .message("Chatbot service is healthy")
                    .data(health)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Chatbot service health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Chatbot service is unavailable: " + e.getMessage())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        }
    }

    /**
     * Get current customer email from JWT token
     */
    private String getCurrentCustomerEmail() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUsername(); // This should be the email
    }

    /**
     * Get current JWT token from request header
     */
    private String getCurrentAuthToken() {
        // This is a simplified approach - in a real implementation, you might want to 
        // extract the token from the HttpServletRequest or store it in the SecurityContext
        return null; // Will be handled by the Python service authentication if needed
    }
}