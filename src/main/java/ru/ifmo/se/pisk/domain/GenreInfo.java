package ru.ifmo.se.pisk.domain;

public record GenreInfo(
        Long id,
        String title,
        String description,
        String color,
        double averageRating
) {
}
