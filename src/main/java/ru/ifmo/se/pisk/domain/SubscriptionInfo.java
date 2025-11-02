package ru.ifmo.se.pisk.domain;

import java.time.Instant;

public record SubscriptionInfo(
        Long id,
        Long followerId,
        Long artistId,
        SubscriptionStatus status,
        Instant createdAt
) {
}
