package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotNull;
import ru.ifmo.se.pisk.domain.TrackStatus;

public record TrackStatusUpdateRequest(
        @NotNull TrackStatus status,
        @NotNull Long moderatorId
) {
}
