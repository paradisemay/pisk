package ru.ifmo.se.pisk.web.view;

import java.time.Instant;

public record AlbumCardView(
        Long id,
        String name,
        String coverPublicUrl,
        int trackCount,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        Long genreId,
        String genreTitle,
        Long uploaderId,
        String uploaderName,
        Long artistId,
        String artistName,
        Instant uploadDate
) {
}
