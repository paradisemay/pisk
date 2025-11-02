package ru.ifmo.se.pisk.domain;

import java.time.Instant;
import java.util.Set;

public record UserProfile(
        Long id,
        String username,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant lastLoginAt,
        Set<Long> subscriptionArtistIds
) {
}
