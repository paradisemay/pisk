package ru.ifmo.se.pisk.repository.projection;

import java.time.LocalDateTime;

public interface TrackSearchResult {
    Long getTrackId();
    String getTrackName();
    String getArtistName();
    String getAlbumName();
    String getGenreTitle();
    Long getRatingSum();
    Integer getRateCount();
    Double getRatingAvg();
    LocalDateTime getPublicationDate();
    String getCoverUrl();
}
