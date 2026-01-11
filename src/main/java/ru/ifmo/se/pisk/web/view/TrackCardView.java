package ru.ifmo.se.pisk.web.view;

public record TrackCardView(
        Long id,
        String name,
        String artistName,
        String albumName,
        String coverUrl,
        Double rating,
        int rank
) {
}
