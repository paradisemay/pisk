package ru.ifmo.se.pisk.service.dto;

import java.util.function.Supplier;
import java.io.InputStream;

public record TrackUploadCommand(
        String name,
        long durationSeconds,
        Long albumId,
        Long genreId,
        boolean explicit,
        boolean inoagent,
        int numberOnAlbum,
        String contentType,
        long fileSize,
        String originalFilename,
        Supplier<InputStream> dataSupplier,
        Long coverSize,
        String coverContentType,
        String coverOriginalFilename,
        Supplier<InputStream> coverSupplier
) {
}
