package ru.ifmo.se.pisk.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    private final RateLimitService rateLimitService = new RateLimitService();

    @Test
    void repeatedCallsWithinIntervalThrow() {
        rateLimitService.checkRateLimit("key");
        assertThrows(IllegalStateException.class, () -> rateLimitService.checkRateLimit("key"));
    }
}
