package ru.ifmo.se.pisk.service.dto;

import java.util.function.Supplier;
import java.io.InputStream;

public record TrackUploadCommand(
        String name,
        String artistName,
        String genreName,
        String albumName,
        long durationSeconds,
        boolean explicit,
        boolean inoagent,
        int numberOnAlbum,
        String contentType,
        long fileSize,
        String originalFilename,
        Supplier<InputStream> dataSupplier,
        byte[] coverData,
        String coverContentType
) {
}
