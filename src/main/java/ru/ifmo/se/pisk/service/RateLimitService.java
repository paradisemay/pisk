package ru.ifmo.se.pisk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, Instant> lastOperation = new ConcurrentHashMap<>();
    private final Duration defaultInterval = Duration.ofSeconds(5);

    public void checkRateLimit(String key) {
        Instant now = Instant.now();
        Instant previous = lastOperation.put(key, now);
        if (previous != null && Duration.between(previous, now).compareTo(defaultInterval) < 0) {
            throw new IllegalStateException("Превышен лимит частоты операций");
        }
    }
}
