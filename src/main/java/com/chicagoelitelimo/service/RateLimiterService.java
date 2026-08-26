package com.chicagoelitelimo.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Simple in-memory sliding-window limiter to slow down brute-forcing of booking reference + phone lookups.
@Component
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        Window window = windows.computeIfAbsent(key, k -> new Window());
        return window.tryConsume();
    }

    private static class Window {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private int count = 0;

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() > WINDOW_MILLIS) {
                windowStart.set(now);
                count = 0;
            }
            count++;
            return count <= MAX_ATTEMPTS;
        }
    }
}
