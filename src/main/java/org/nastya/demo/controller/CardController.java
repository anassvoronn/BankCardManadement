package org.nastya.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@Slf4j
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public Page<CardDto> getAll(Pageable pageable) {
        log.info("fetching all cards");
        return cardService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public CardDto getById(@PathVariable UUID id) {
        log.info("fetching card by id={}", id);
        return cardService.getById(id);
    }

    @GetMapping("/private")
    public Page<CardDto> getAllOfCurrentUser(Pageable pageable) {
        log.info("fetching own cards");
        return cardService.getAllOfCurrentUser(pageable);
    }

    @GetMapping("/private/{id}")
    public CardDto getByIdOfCurrentUser(@PathVariable UUID id) {
        log.info("fetching own card by id={}", id);
        return cardService.getByIdOfCurrentUser(id);
    }

    @PostMapping
    public CardDto createCard(@Valid @RequestBody CardCreateDto dto) {
        log.info("Creating card for userId={}", dto.userId());
        return cardService.create(dto);
    }

    @PutMapping("/{id}")
    public CardDto updateCard(@PathVariable UUID id, @Valid @RequestBody CardCreateDto dto) {
        log.info("Updating card id={}", id);
        return cardService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteCard(@PathVariable UUID id) {
        log.info("Deleting card id={}", id);
        cardService.delete(id);
    }

    @PutMapping("/transfer")
    public void transferBetweenOwnCards(
            @Valid @RequestBody TransferDto dto) {
        cardService.transferBetweenOwnCards(dto);
    }

    @PutMapping("/{id}/status")
    public void changeCardStatus(@PathVariable UUID id, @RequestParam CardStatus status) {
        cardService.changeCardStatus(id, status);
    }

    @PutMapping("/private/{id}/block")
    public void blockCardOfCurrentUser(@PathVariable UUID id) {
        cardService.blockCardOfCurrentUser(id);
    }

    @GetMapping("/private/{id}/balance")
    public BigDecimal getCardBalanceOfCurrentUser(@PathVariable UUID id) {
        return cardService.getCardBalanceOfCurrentUser(id);
    }
}
