package com.honklimo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    private final ConcurrentHashMap<String, List<Instant>> requestCounts = new ConcurrentHashMap<>();
    
    // Max 3 bookings per 24 hours per IP/Phone
    private static final int MAX_REQUESTS = 3;
    private static final Duration TIME_WINDOW = Duration.ofHours(24);

    public boolean isAllowed(String key) {
        return isAllowed(key, MAX_REQUESTS);
    }

    public boolean isAllowed(String key, int maxRequests) {
        if (key == null || key.trim().isEmpty()) {
            return true; // Don't block if key is somehow missing
        }
        
        Instant now = Instant.now();
        
        List<Instant> timestamps = requestCounts.compute(key, (k, existingTimestamps) -> {
            if (existingTimestamps == null) {
                existingTimestamps = new ArrayList<>();
            }
            // Remove timestamps outside the 24-hour window
            existingTimestamps.removeIf(t -> t.isBefore(now.minus(TIME_WINDOW)));
            
            // Add the current request
            existingTimestamps.add(now);
            return existingTimestamps;
        });
        
        boolean allowed = timestamps.size() <= maxRequests;
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for key: {} (Limit: {})", key, maxRequests);
        }
        
        return allowed;
    }
    
    // Run cleanup every hour to prevent memory leaks
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        logger.info("Running RateLimitingService cleanup...");
        Instant now = Instant.now();
        
        requestCounts.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(t -> t.isBefore(now.minus(TIME_WINDOW)));
            return entry.getValue().isEmpty();
        });
    }
}
