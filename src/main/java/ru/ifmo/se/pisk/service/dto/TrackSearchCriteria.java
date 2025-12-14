package ru.ifmo.se.pisk.service.dto;

import java.util.Set;
import ru.ifmo.se.pisk.domain.TrackStatus;

public record TrackSearchCriteria(
        String query,
        Set<Long> genreIds,
        Long authorId,
        TrackStatus status,
        Boolean explicit,
        Boolean inoagent
) {
}
