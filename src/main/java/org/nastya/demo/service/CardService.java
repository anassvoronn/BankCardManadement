package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.dto.CardStatusDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.repository.CardRepository;
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
    private final CardMapper cardMapper;

    public CardDto getById(UUID id) {
        log.info("Fetching card by id={}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Card not found, id={}", id);
                    return new EntityNotFoundException("Card not found");
                });

        return cardMapper.toDto(card);
    }

    public Page<CardDto> getAll(Pageable pageable) {
        log.info("Fetching all cards");
        return cardRepository.findAll(pageable)
                .map(cardMapper::toDto);
    }

    public CardDto create(CardDto dto) {
        log.info("Creating new card for userId={}", dto.userId());
        validateCard(dto);

        Card card = cardMapper.toEntity(dto);
        card.setBalance(dto.balance() != null ? dto.balance() : BigDecimal.ZERO);

        Card saved = cardRepository.save(card);
        log.info("Card created successfully, id={}", saved.getId());

        return cardMapper.toDto(saved);
    }

    public CardDto update(UUID id, CardDto dto) {
        log.info("Updating card id={}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());
        card.setStatus(dto.status());

        Card updated = cardRepository.save(card);
        log.info("Card updated successfully, id={}", updated.getId());

        return cardMapper.toDto(updated);
    }

    public void delete(UUID id) {
        log.info("Deleting card id={}", id);

        if (!cardRepository.existsById(id)) {
            log.warn("Card not found for delete, id={}", id);
            throw new EntityNotFoundException("Card not found");
        }

        cardRepository.deleteById(id);
        log.info("Card deleted successfully, id={}", id);
    }

    public BigDecimal getCardBalance(UUID cardId, UUID userId) {
        log.info("Requesting balance for cardId={}, userId={}", cardId, userId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> {
                    log.warn("Card not found or access denied. cardId={}, userId={}", cardId, userId);
                    return new IllegalArgumentException("Card not found or access denied");
                });

        if (card.getStatus() != CardStatus.ACTIVE) {
            log.warn("Attempt to view balance of inactive card. cardId={}, status={}", cardId, card.getStatus());
            throw new IllegalStateException("Card is not active");
        }

        log.debug("Balance retrieved successfully for cardId={}", cardId);
        return card.getBalance();
    }

    @Transactional
    public void transferBetweenOwnCards(TransferDto dto) {
        log.info("Transfer requested: userId={}, fromCard={}, toCard={}, amount={}",
                dto.userId(), dto.fromCardId(), dto.toCardId(), dto.amount());

        validateTransfer(dto);

        Card from = cardRepository.findByIdAndUserId(dto.fromCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Source card not found"));

        Card to = cardRepository.findByIdAndUserId(dto.toCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Target card not found"));

        validateTransferCards(from, to, dto.amount());

        from.setBalance(from.getBalance().subtract(dto.amount()));
        to.setBalance(to.getBalance().add(dto.amount()));

        log.info("Transfer completed successfully: fromCard={}, toCard={}, amount={}",
                from.getId(), to.getId(), dto.amount());
    }

    public void changeCardStatus(CardStatusDto cardStatusDto) {
        log.info("Request to change status of card id={} to {}", cardStatusDto.id(), cardStatusDto.status());

        Card card = cardRepository.findByIdAndUserId(cardStatusDto.id(), cardStatusDto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setStatus(cardStatusDto.status());
        log.info("Card status changed successfully: id={}, status={}", card.getId(), cardStatusDto.status());
    }

    private void validateCard(CardDto dto) {
        if (dto.encryptedNumber() == null || dto.encryptedNumber().isBlank()) {
            throw new IllegalArgumentException("Card number must not be empty");
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

    private void validateTransferCards(Card from, Card to, BigDecimal amount) {
        if (from.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active");
        }
        if (to.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Target card is not active");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }
}
