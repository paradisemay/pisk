package ru.ifmo.se.pisk.web.view;

import java.time.Instant;

public record AlbumDetailsView(
        Long id,
        String name,
        String description,
        String coverPublicUrl,
        String genreTitle,
        int trackCount,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        String artistName,
        Long uploaderId,
        String uploaderName,
        Instant uploadDate
) {
}
