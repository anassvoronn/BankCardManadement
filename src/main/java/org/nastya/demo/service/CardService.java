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
import org.nastya.demo.service.validation.CardValidator;
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
    private final CardValidator cardValidator;

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
        cardValidator.validateCreate(dto);

        Card card = new Card();
        card.setEncryptedNumber(encryptionService.encrypt(dto.encryptedNumber()));
        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setUser(userRepository.getReferenceById(dto.userId()));

        return mapToDto(cardRepository.save(card));
    }

    public CardDto update(UUID id, CardCreateDto dto) {
        cardValidator.validateCreate(dto);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());

        return mapToDto(cardRepository.save(card));
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
        cardValidator.validateTransfer(dto);

        Card from = cardRepository.findByIdAndUserId(dto.fromCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Source card not found"));

        Card to = cardRepository.findByIdAndUserId(dto.toCardId(), dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Target card not found"));

        cardValidator.validateCardsAreActive(from, to);
        cardValidator.validateSufficientFunds(from, dto.amount());

        from.setBalance(from.getBalance().subtract(dto.amount()));
        to.setBalance(to.getBalance().add(dto.amount()));

        cardRepository.save(from);
        cardRepository.save(to);
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
}
