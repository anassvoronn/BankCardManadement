package org.nastya.demo.dto;

import jakarta.validation.constraints.NotNull;
import org.nastya.demo.enums.CardStatus;
import java.util.UUID;

public record CardStatusDto(@NotNull UUID id,
                            @NotNull UUID userId,
                            @NotNull CardStatus status) {
}
