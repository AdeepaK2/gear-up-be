package com.ead.gearup.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration using Bucket4j
 * Implements token bucket algorithm to prevent DoS attacks
 */
@Configuration
@Component
public class RateLimitConfig {

    // In-memory cache for user buckets (use Redis for production distributed systems)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Get or create a rate limit bucket for a user
     *
     * Rate limits:
     * - 20 requests per minute per user for chat endpoints
     * - Refills at 20 tokens per minute
     *
     * @param userEmail User identifier
     * @return Bucket for the user
     */
    public Bucket resolveBucket(String userEmail) {
        return cache.computeIfAbsent(userEmail, k -> createNewBucket());
    }

    /**
     * Create a new bucket with rate limit configuration
     *
     * Configuration:
     * - Capacity: 20 tokens (burst capacity)
     * - Refill: 20 tokens per minute (1 token every 3 seconds)
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            20,  // capacity - maximum burst
            Refill.intervally(20, Duration.ofMinutes(1))  // refill rate
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Check if request is allowed for a user
     *
     * @param userEmail User identifier
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsume(String userEmail) {
        Bucket bucket = resolveBucket(userEmail);
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining tokens for a user
     *
     * @param userEmail User identifier
     * @return Number of available tokens
     */
    public long getAvailableTokens(String userEmail) {
        Bucket bucket = resolveBucket(userEmail);
        return bucket.getAvailableTokens();
    }
}
