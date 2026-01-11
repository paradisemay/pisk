package ru.ifmo.se.pisk.web.view;

import java.time.Instant;
import ru.ifmo.se.pisk.web.util.DurationFormatter;

public record TrackDetailsView(
        Long id,
        String name,
        Long userId,
        String userName,
        String artistName,
        String genreTitle,
        boolean explicit,
        boolean inoagent,
        double averageRating,
        int rateCount,
        long durationSeconds,
        long fileSize,
        Instant uploadDate,
        String downloadUrl
) {

    public String formattedDuration() {
        return DurationFormatter.formatSeconds(durationSeconds);
    }
}
