package ru.ifmo.se.pisk.controller.dto;

import java.time.Instant;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;

public record SubscriptionResponse(
        Long id,
        Long followerId,
        Long artistId,
        SubscriptionStatus status,
        Instant createdAt
) {
}
