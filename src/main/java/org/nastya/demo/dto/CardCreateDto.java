package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateDto(
        @NotNull String encryptedNumber,
        @NotNull String ownerName,
        @NotNull LocalDate expiryDate,
        @NotNull UUID userId) {
}
