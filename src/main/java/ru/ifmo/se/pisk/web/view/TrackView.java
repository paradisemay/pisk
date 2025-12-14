package ru.ifmo.se.pisk.web.view;

import java.time.Instant;
import ru.ifmo.se.pisk.web.util.DurationFormatter;

public record TrackView(
        Long id,
        String name,
        Long authorId,
        String authorName,
        String genreTitle,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        long durationSeconds,
        Instant uploadDate
) {

    public String formattedDuration() {
        return DurationFormatter.formatSeconds(durationSeconds);
    }
}
