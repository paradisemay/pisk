package ru.ifmo.se.pisk.web.util;

public final class DurationFormatter {

    private DurationFormatter() {
    }

    public static String formatSeconds(long durationSeconds) {
        long minutes = Math.floorDiv(durationSeconds, 60);
        long seconds = Math.floorMod(durationSeconds, 60);
        String secondsPadded = String.format("%02d", seconds);
        return minutes + ":" + secondsPadded;
    }
}
