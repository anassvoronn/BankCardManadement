package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import org.nastya.demo.enums.Role;

public record UserDto(@NotNull String username, @NotNull String password, @NotNull Role role) {
}
