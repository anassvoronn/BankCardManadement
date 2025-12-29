package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.dto.CardStatusDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.repository.CardRepository;
import org.nastya.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public CardDto getById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        return mapToDto(card);
    }


    public Page<CardDto> getAll(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    public CardDto create(CardCreateDto dto) {
        validateCardCreateDto(dto);

        Card card = new Card();
        card.setEncryptedNumber(encryptionService.encrypt(dto.encryptedNumber()));
        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setUser(userRepository.getReferenceById(dto.userId()));

        Card saved = cardRepository.save(card);
        return mapToDto(saved);
    }

    public CardDto update(UUID id, CardCreateDto dto) {
        validateCardCreateDto(dto);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());

        Card updated = cardRepository.save(card);
        return mapToDto(updated);
    }

    public void delete(UUID id) {
        if (!cardRepository.existsById(id)) {
            throw new EntityNotFoundException("Card not found");
        }
        cardRepository.deleteById(id);
    }

    public BigDecimal getCardBalance(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found or access denied"));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Card is not active");
        }
        return card.getBalance();
    }

    @Transactional
    public void transferBetweenOwnCards(TransferDto dto) {
        validateTransfer(dto);

        Card from = cardRepository.findByIdAndUserId(dto.fromCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Source card not found"));

        Card to = cardRepository.findByIdAndUserId(dto.toCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Target card not found"));

        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("One of the cards is not active");
        }
        if (from.getBalance().compareTo(dto.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(dto.amount()));
        to.setBalance(to.getBalance().add(dto.amount()));
    }

    public void changeCardStatus(CardStatusDto cardStatusDto) {
        log.info("Request to change status of card id={} to {}", cardStatusDto.id(), cardStatusDto.status());

        Card card = cardRepository.findByIdAndUserId(cardStatusDto.id(), cardStatusDto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setStatus(cardStatusDto.status());
        log.info("Card status changed successfully: id={}, status={}", card.getId(), cardStatusDto.status());
    }

    private CardDto mapToDto(Card card) {
        String plain = encryptionService.decrypt(card.getEncryptedNumber());
        String masked = encryptionService.maskCardNumber(plain);

        return new CardDto(
                masked,
                card.getOwnerName(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId()
        );
    }

    private void validateCardCreateDto(CardCreateDto dto) {
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

    private void validateTransfer(TransferDto dto) {
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (dto.fromCardId().equals(dto.toCardId())) {
            throw new IllegalArgumentException("Source and target cards must be different");
        }
    }
}
