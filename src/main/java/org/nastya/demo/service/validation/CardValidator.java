package org.nastya.demo.service.validation;

import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.enums.CardStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardValidator {

    public void validateCreate(CardCreateDto dto) {
        if (dto.encryptedNumber() == null || dto.encryptedNumber().isBlank()) {
            throw new IllegalArgumentException("Card number must not be empty");
        }
        if (!dto.encryptedNumber().matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        if (dto.ownerName() == null || dto.ownerName().isBlank()) {
            throw new IllegalArgumentException("Owner name must not be empty");
        }
        if (dto.userId() == null) {
            throw new IllegalArgumentException("Card must be assigned to a user");
        }
    }

    public void validateTransfer(TransferDto dto) {
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (dto.fromCardId().equals(dto.toCardId())) {
            throw new IllegalArgumentException("Source and target cards must be different");
        }
    }

    public void validateCardsAreActive(Card from, Card to) {
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("One of the cards is not active");
        }
    }

    public void validateSufficientFunds(Card from, BigDecimal amount) {
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }
}
