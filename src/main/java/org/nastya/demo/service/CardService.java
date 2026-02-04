package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.entity.User;
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
    private final CardMapper cardMapper;
    private final EncryptionService encryptionService;
    private final CardValidator cardValidator;
    private final CustomUserDetailsService customUserDetailsService;

    @Transactional(readOnly = true)
    public CardDto getById(UUID id) {
        log.info("fetching card by id={}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getAll(Pageable pageable) {
        log.info("fetching all cards");

        return cardRepository.findAll(pageable)
                .map(cardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CardDto getByIdOfCurrentUser(UUID id) {
        User currentUser = customUserDetailsService.getCurrentUser();
        log.info("fetching card by id={}, userId={}", id, currentUser.getId());

        Card card = cardRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Card not found or access denied"));

        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getAllOfCurrentUser(Pageable pageable) {
        User currentUser = customUserDetailsService.getCurrentUser();
        log.info("fetching cards for userId={}", currentUser.getId());

        return cardRepository.findAllByUserId(currentUser.getId(), pageable)
                .map(cardMapper::toDto);
    }

    public CardDto create(CardCreateDto dto) {
        cardValidator.validateCreate(dto);

        Card card = new Card();
        card.setEncryptedNumber(encryptionService.encrypt(dto.cardNumber()));
        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setUser(userRepository.getReferenceById(dto.userId()));

        Card savedCard = cardRepository.save(card);
        return cardMapper.toDto(savedCard);
    }

    @Transactional
    public CardDto update(UUID id, CardCreateDto dto) {
        cardValidator.validateCreate(dto);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());

        return cardMapper.toDto(card);
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

    public BigDecimal getCardBalanceOfCurrentUser(UUID cardId) {
        User user = customUserDetailsService.getCurrentUser();

        log.info("Requesting balance for cardId={}, userId={}", cardId, user.getId());

        Card card = cardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Card not found or access denied"));

        if (card.getStatus() != CardStatus.ACTIVE) {
            log.warn("Attempt to view balance of inactive card. cardId={}, status={}",
                    cardId, card.getStatus());
            throw new IllegalStateException("Card is not active");
        }

        log.debug("Balance retrieved successfully for cardId={}", cardId);
        return card.getBalance();
    }

    @Transactional
    public void transferBetweenOwnCards(TransferDto dto) {
        User user = customUserDetailsService.getCurrentUser();
        log.info("Transfer requested: userId={}, fromCard={}, toCard={}, amount={}",
                user.getId(), dto.fromCardId(), dto.toCardId(), dto.amount());

        cardValidator.validateTransfer(dto);

        Card from = cardRepository.findByIdAndUserId(dto.fromCardId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Source card not found"));

        Card to = cardRepository.findByIdAndUserId(dto.toCardId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Target card not found"));

        cardValidator.validateCardsAreActive(from, to);
        cardValidator.validateSufficientFunds(from, dto.amount());

        from.setBalance(from.getBalance().subtract(dto.amount()));
        to.setBalance(to.getBalance().add(dto.amount()));
    }

    @Transactional
    public void changeCardStatus(UUID id, CardStatus status) {
        log.info("changing card status: cardId={}, status={}", id, status);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setStatus(status);

        log.info("Card status changed by ADMIN: id={}, status={}", card.getId(), status);
    }

    @Transactional
    public void blockCardOfCurrentUser(UUID cardId) {
        User user = customUserDetailsService.getCurrentUser();
        log.info("blocking card: userId={}, cardId={}", user.getId(), cardId);

        Card card = cardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Card not found or access denied"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            log.warn("Card already blocked: cardId={}", cardId);
            return;
        }

        card.setStatus(CardStatus.BLOCKED);

        log.info("Card blocked successfully: cardId={}", cardId);
    }
}