package ru.ifmo.se.pisk.repository.projection;

import java.time.Instant;

/**
 * Проекция для карточек альбомов в каталоге.
 *
 * artist/uploader берутся по первому треку (numberOnAlbum = 1).
 */
public record AlbumCardAggregate(
        Long albumId,
        String albumName,
        String coverUrl,
        Instant uploadDate,
        int trackNumber,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        Long genreId,
        String genreTitle,
        Long uploaderId,
        String uploaderName,
        Long artistId,
        String artistName
) {
}
