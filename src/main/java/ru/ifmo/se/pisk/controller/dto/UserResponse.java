package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;
import java.util.Set;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;

public record UserResponse(
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
