package ru.ifmo.se.pisk.service.dto;

import ru.ifmo.se.pisk.domain.RatingInfo;

public record RateResult(RatingInfo ratingInfo, boolean trackDeleted) {
}
