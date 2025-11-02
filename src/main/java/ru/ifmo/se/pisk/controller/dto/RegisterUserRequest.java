package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.ifmo.se.pisk.domain.UserRole;

public record RegisterUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull UserRole role
) {
}
