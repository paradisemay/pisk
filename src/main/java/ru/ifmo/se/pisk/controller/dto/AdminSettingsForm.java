package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminSettingsForm {
    @NotNull(message = "{validation.moderation.required}")
    private Boolean moderationEnabled = Boolean.TRUE;

    @Min(value = 1, message = "{validation.pageSize.min}")
    @Max(value = 100, message = "{validation.pageSize.max}")
    private Integer defaultPageSize = 10;

    @NotNull(message = "{validation.deletionThreshold.required}")
    @DecimalMin(value = "1.0", message = "{validation.deletionThreshold.min}")
    @DecimalMax(value = "5.0", message = "{validation.deletionThreshold.max}")
    private Double deletionThreshold = 3.0;
}
