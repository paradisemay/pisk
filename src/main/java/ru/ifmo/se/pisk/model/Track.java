package ru.ifmo.se.pisk.model;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class Track {
    Long id;
    String title;
    String description;
    String audioUrl;
    String coverUrl;
    String genre;
    List<String> tags;
    double rating;
    int plays;
    LocalDate publishedAt;
    Author author;
    Duration duration;
}
