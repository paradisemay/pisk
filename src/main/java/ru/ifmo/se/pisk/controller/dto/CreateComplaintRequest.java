package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotNull Long trackId,
        @NotNull Long complainantId,
        @NotBlank @Size(max = 512) String reason
) {
}
