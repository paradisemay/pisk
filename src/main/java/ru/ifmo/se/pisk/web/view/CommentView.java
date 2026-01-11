package ru.ifmo.se.pisk.web.view;

public record CommentView(
    Long id,
    String authorName,
    String authorAvatarUrl,
    String body,
    Integer trackTime,
    String formattedTrackTime,
    String createdAt
) {}
