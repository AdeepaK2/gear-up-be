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

import com.ead.gearup.config.RateLimitConfig;
import com.ead.gearup.dto.chatbot.ChatRequest;
import com.ead.gearup.dto.chatbot.ChatResponse;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.service.AuditLogService;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.model.User;

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
    private final RateLimitConfig rateLimitConfig;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

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
            HttpServletRequest httpRequest,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            String questionPreview = request.getQuestion().substring(0, Math.min(50, request.getQuestion().length()));
            log.info("Processing chat request: {}", questionPreview);

            // Get authenticated customer context
            String customerEmail = getCurrentCustomerEmail();

            // Rate limiting check
            if (!rateLimitConfig.tryConsume(customerEmail)) {
                log.warn("Rate limit exceeded for user: {}", customerEmail);
                auditLogService.logRateLimitViolation(customerEmail, "/chat");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponseDTO.<ChatResponse>builder()
                                .status("error")
                                .message("Rate limit exceeded. Please try again in a few moments.")
                                .timestamp(Instant.now())
                                .path(httpRequest.getRequestURI())
                                .build());
            }

            Long customerId = customerService.getCustomerIdByEmail(customerEmail);

            // Audit log - chat request initiated
            auditLogService.logChatRequest(customerEmail, questionPreview, true);

            // Extract JWT token from Authorization header
            String jwtToken = extractJwtToken(authorizationHeader);

            // Prepare request for Python chatbot service
            var chatbotRequest = com.ead.gearup.dto.chatbot.ChatbotServiceRequest.builder()
                    .question(request.getQuestion())
                    .sessionId(request.getSessionId())
                    .conversationHistory(request.getConversationHistory())
                    .customerId(customerId)
                    .customerEmail(customerEmail)
                    .authToken(jwtToken)
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
            try {
                String customerEmail = getCurrentCustomerEmail();
                auditLogService.logChatRequest(customerEmail, "", false);
            } catch (Exception ignored) {
                // Best effort audit logging
            }
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.<ChatResponse>builder()
                            .status("error")
                            .message("Chatbot service error: " + e.getResponseBodyAsString())
                            .timestamp(Instant.now())
                            .path(httpRequest.getRequestURI())
                            .build());
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            try {
                String customerEmail = getCurrentCustomerEmail();
                auditLogService.logChatRequest(customerEmail, "", false);
            } catch (Exception ignored) {
                // Best effort audit logging
            }
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

            // Get authenticated customer context
            String customerEmail = getCurrentCustomerEmail();

            // Rate limiting check
            if (!rateLimitConfig.tryConsume(customerEmail)) {
                log.warn("Rate limit exceeded for user: {}", customerEmail);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Rate limit exceeded. Please try again in a few moments.");
            }

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
            Long userId = getCurrentUserId();
            log.info("Getting chat sessions for customer: {} (user_id: {})", customerEmail, userId);

            WebClient webClient = webClientBuilder.build();
            String sessionsUri = chatbotServiceUrl + "/chat/sessions?limit=" + limit + "&customerEmail=" + customerEmail;
            if (userId != null) {
                sessionsUri += "&user_id=" + userId;
            }

            Object sessions = webClient
                    .get()
                    .uri(sessionsUri)
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

            // Audit log - session creation
            auditLogService.logSessionOperation(customerEmail, "CREATE", "new", true);

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

            String authenticatedEmail = getCurrentCustomerEmail();

            // Audit log - session deletion attempt
            auditLogService.logSessionOperation(authenticatedEmail, "DELETE", sessionId, false);

            // Proceed with deletion directly without ownership verification
            WebClient webClient = webClientBuilder.build();
            Object result = webClient
                    .delete()
                    .uri(chatbotServiceUrl + "/chat/sessions/" + sessionId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            log.info("Successfully deleted session {} for user {}", sessionId, authenticatedEmail);

            // Audit log - successful deletion
            auditLogService.logSessionOperation(authenticatedEmail, "DELETE", sessionId, true);

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
     * Verify that a session belongs to the authenticated user
     */
    private boolean verifySessionOwnership(Object sessionsResponse, String sessionId) {
        try {
            // Parse the sessions response and check if sessionId exists
            // This is a simplified check - you might need to adjust based on actual response structure
            if (sessionsResponse != null) {
                String responseString = sessionsResponse.toString();
                return responseString.contains(sessionId);
            }
            return false;
        } catch (Exception e) {
            log.error("Error verifying session ownership", e);
            return false; // Fail secure: deny access if verification fails
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
     * Get current customer/user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            String email = getCurrentCustomerEmail();
            // Get user by email to find user ID
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                return user.getUserId();
            }
            log.warn("Could not find user ID for email: {}", email);
            return null;
        } catch (Exception e) {
            log.error("Error extracting user ID from security context", e);
            return null;
        }
    }

    /**
     * Get current JWT token from request header
     */
    /**
     * Extract JWT token from Authorization header
     *
     * @param authorizationHeader The Authorization header value (e.g., "Bearer eyJhbGc...")
     * @return The JWT token without the "Bearer " prefix, or null if invalid
     */
    private String extractJwtToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Remove "Bearer " prefix
        }
        log.warn("Invalid or missing Authorization header");
        return null;
    }
}