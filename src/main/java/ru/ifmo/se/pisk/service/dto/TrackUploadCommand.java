package ru.ifmo.se.pisk.service.dto;

import java.util.Set;
import java.util.function.Supplier;
import java.io.InputStream;

public record TrackUploadCommand(
        String title,
        long durationSeconds,
        Long albumId,
        Set<Long> genreIds,
        String contentType,
        long fileSize,
        String originalFilename,
        Supplier<InputStream> dataSupplier
) {
}
