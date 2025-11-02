package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotNull;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;

public record UpdateSubscriptionStatusRequest(
        @NotNull SubscriptionStatus status,
        @NotNull Long moderatorId
) {
}
