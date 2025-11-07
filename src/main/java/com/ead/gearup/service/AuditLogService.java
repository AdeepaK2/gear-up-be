package com.ead.gearup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Audit Logging Service for Security Events
 *
 * Logs security-relevant events for monitoring and compliance:
 * - Chat access attempts
 * - Appointment queries
 * - Session operations
 * - Rate limit violations
 * - Authorization failures
 */
@Service
@Slf4j
public class AuditLogService {

    /**
     * Log chat message request
     *
     * @param customerEmail User making the request
     * @param questionPreview First 50 chars of question
     * @param success Whether request succeeded
     */
    public void logChatRequest(String customerEmail, String questionPreview, boolean success) {
        log.info("AUDIT - CHAT_REQUEST | user={} | question={} | success={} | timestamp={}",
                customerEmail,
                questionPreview,
                success,
                Instant.now());
    }

    /**
     * Log appointment data access
     *
     * @param customerEmail User making the request
     * @param operation Operation type (VIEW, CREATE, UPDATE, DELETE)
     * @param appointmentId Appointment ID (if applicable)
     * @param success Whether request succeeded
     */
    public void logAppointmentAccess(String customerEmail, String operation, Long appointmentId, boolean success) {
        log.info("AUDIT - APPOINTMENT_ACCESS | user={} | operation={} | appointmentId={} | success={} | timestamp={}",
                customerEmail,
                operation,
                appointmentId,
                success,
                Instant.now());
    }

    /**
     * Log chat session operation
     *
     * @param customerEmail User making the request
     * @param operation Operation type (CREATE, DELETE, VIEW)
     * @param sessionId Session ID
     * @param success Whether request succeeded
     */
    public void logSessionOperation(String customerEmail, String operation, String sessionId, boolean success) {
        log.info("AUDIT - SESSION_OPERATION | user={} | operation={} | sessionId={} | success={} | timestamp={}",
                customerEmail,
                operation,
                sessionId,
                success,
                Instant.now());
    }

    /**
     * Log rate limit violation
     *
     * @param customerEmail User who exceeded rate limit
     * @param endpoint Endpoint being accessed
     */
    public void logRateLimitViolation(String customerEmail, String endpoint) {
        log.warn("AUDIT - RATE_LIMIT_EXCEEDED | user={} | endpoint={} | timestamp={}",
                customerEmail,
                endpoint,
                Instant.now());
    }

    /**
     * Log authorization failure
     *
     * @param customerEmail User who attempted unauthorized access
     * @param operation Operation attempted
     * @param resourceId Resource ID (if applicable)
     * @param reason Reason for failure
     */
    public void logAuthorizationFailure(String customerEmail, String operation, String resourceId, String reason) {
        log.warn("AUDIT - AUTHORIZATION_FAILURE | user={} | operation={} | resourceId={} | reason={} | timestamp={}",
                customerEmail,
                operation,
                resourceId,
                reason,
                Instant.now());
    }

    /**
     * Log vector database query
     *
     * @param customerEmail User making the query
     * @param resultsCount Number of results returned
     */
    public void logVectorSearch(String customerEmail, int resultsCount) {
        log.info("AUDIT - VECTOR_SEARCH | user={} | resultsCount={} | timestamp={}",
                customerEmail,
                resultsCount,
                Instant.now());
    }

    /**
     * Log authentication event
     *
     * @param customerEmail User attempting authentication
     * @param success Whether authentication succeeded
     * @param method Authentication method (LOGIN, JWT_REFRESH, etc.)
     */
    public void logAuthentication(String customerEmail, boolean success, String method) {
        log.info("AUDIT - AUTHENTICATION | user={} | method={} | success={} | timestamp={}",
                customerEmail,
                method,
                success,
                Instant.now());
    }
}
