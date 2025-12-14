package ru.ifmo.se.pisk.web.view;

public record ProfileView(
        Long id,
        String username,
        String email,
        String avatarUrl
) {
}
