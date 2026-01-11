package ru.ifmo.se.pisk.web.view;

import java.time.Instant;
import ru.ifmo.se.pisk.web.util.DurationFormatter;

public record AlbumTrackView(
        Long id,
        int numberOnAlbum,
        String name,
        long durationSeconds,
        double averageRating,
        boolean explicit,
        boolean inoagent,
        Instant uploadDate
) {

    public String formattedDuration() {
        return DurationFormatter.formatSeconds(durationSeconds);
    }
}
