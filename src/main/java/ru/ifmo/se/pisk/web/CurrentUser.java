package ru.ifmo.se.pisk.web;

import ru.ifmo.se.pisk.domain.UserRole;

public record CurrentUser(
        Long id,
        String username,
        String avatarUrl,
        UserRole role
) {
}
