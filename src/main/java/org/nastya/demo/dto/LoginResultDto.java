package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;

public record LoginResultDto(@NotNull String token) {
}