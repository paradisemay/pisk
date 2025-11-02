package ru.ifmo.se.pisk.repository.projection;

public record ArtistRatingAggregate(Long artistId, Double averageRating, Long ratingsCount) {
}
