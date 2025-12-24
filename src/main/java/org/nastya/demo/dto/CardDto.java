package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import org.nastya.demo.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardDto(
        @NotNull String encryptedNumber,
        @NotNull String ownerName,
        @NotNull LocalDate expiryDate,
        @NotNull CardStatus status,
        @NotNull BigDecimal balance,
        @NotNull UUID userId) {
}
