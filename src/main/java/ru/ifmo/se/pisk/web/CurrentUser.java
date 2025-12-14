package ru.ifmo.se.pisk.web;

public record CurrentUser(
        Long id,
        String username,
        String avatarUrl
) {
}
