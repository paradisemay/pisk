package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.ifmo.se.pisk.domain.ComplaintStatus;

public record ResolveComplaintRequest(
        @NotNull ComplaintStatus status,
        @NotNull Long moderatorId,
        @NotBlank String note
) {
}
