package ru.ifmo.se.pisk.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Author {
    Long id;
    String name;
    String bio;
    String avatarUrl;
    LocalDate registeredAt;
    int followers;
}
