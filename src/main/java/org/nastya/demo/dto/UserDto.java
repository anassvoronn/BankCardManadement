package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import org.nastya.demo.enums.Role;

import java.util.UUID;

public record UserDto(@NotNull UUID id, @NotNull String username, @NotNull Role role) {
}
