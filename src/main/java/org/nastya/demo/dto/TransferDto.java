package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferDto(@NotNull UUID userId,
                          @NotNull UUID fromCardId,
                          @NotNull UUID toCardId,
                          @NotNull @Positive BigDecimal amount) {

}
