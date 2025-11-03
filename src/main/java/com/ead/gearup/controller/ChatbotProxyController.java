package com.ead.gearup.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
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
import java.util.Map;

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
     * Get all chat sessions for current customer
     */
    @GetMapping("/sessions")
    @Operation(
        summary = "Get chat sessions",
        description = "Retrieve all chat sessions for the current customer"
    )
    public ResponseEntity<ApiResponseDTO<Object>> getChatSessions(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {

        try {
            String customerEmail = getCurrentCustomerEmail();
            log.info("Getting chat sessions for customer: {}", customerEmail);

            WebClient webClient = webClientBuilder.build();
            Object sessions = webClient
                    .get()
                    .uri(chatbotServiceUrl + "/chat/sessions?limit=" + limit + "&customerEmail=" + customerEmail)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            ApiResponseDTO<Object> response = ApiResponseDTO.<Object>builder()
                    .status("success")
                    .message("Chat sessions retrieved successfully")
                    .data(sessions)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting chat sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Error getting chat sessions: " + e.getMessage())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        }
    }

    /**
     * Create new chat session
     */
    @PostMapping("/sessions")
    @Operation(
        summary = "Create chat session",
        description = "Create a new chat session for the current customer"
    )
    public ResponseEntity<ApiResponseDTO<Object>> createChatSession(
            @RequestParam(required = false) String title,
            HttpServletRequest httpRequest) {

        try {
            String customerEmail = getCurrentCustomerEmail();
            log.info("Creating new chat session for customer: {}", customerEmail);
            log.info("Calling Python service at: {}/chat/sessions", chatbotServiceUrl);

            WebClient webClient = webClientBuilder.build();
            
            Map<String, String> requestBody = Map.of(
                "customerEmail", customerEmail,
                "title", title != null ? title : "New Chat"
            );
            log.info("Request body: {}", requestBody);
            
            Object session = webClient
                    .post()
                    .uri(chatbotServiceUrl + "/chat/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            ApiResponseDTO<Object> response = ApiResponseDTO.<Object>builder()
                    .status("success")
                    .message("Chat session created successfully")
                    .data(session)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            log.error("Python service error - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Python service error: " + e.getResponseBodyAsString())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        } catch (Exception e) {
            log.error("Error creating chat session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Error creating chat session: " + e.getMessage())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        }
    }

    /**
     * Delete chat session
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
        summary = "Delete chat session",
        description = "Delete a chat session and its history"
    )
    public ResponseEntity<ApiResponseDTO<Object>> deleteChatSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        try {
            log.info("Deleting chat session: {}", sessionId);

            WebClient webClient = webClientBuilder.build();
            Object result = webClient
                    .delete()
                    .uri(chatbotServiceUrl + "/chat/sessions/" + sessionId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            ApiResponseDTO<Object> response = ApiResponseDTO.<Object>builder()
                    .status("success")
                    .message("Chat session deleted successfully")
                    .data(result)
                    .timestamp(Instant.now())
                    .path(httpRequest.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting chat session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<Object>builder()
                            .status("error")
                            .message("Error deleting chat session: " + e.getMessage())
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
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("No authentication found in security context");
                throw new RuntimeException("User not authenticated");
            }
            
            Object principal = authentication.getPrincipal();
            log.info("Principal type: {}", principal.getClass().getName());
            
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                String email = userDetails.getUsername();
                log.info("Extracted email from UserDetails: {}", email);
                return email;
            } else if (principal instanceof String) {
                String email = (String) principal;
                log.info("Extracted email from String principal: {}", email);
                return email;
            } else {
                log.error("Unknown principal type: {}", principal.getClass().getName());
                throw new RuntimeException("Unable to extract email from authentication principal");
            }
        } catch (Exception e) {
            log.error("Error extracting customer email from security context", e);
            throw e;
        }
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