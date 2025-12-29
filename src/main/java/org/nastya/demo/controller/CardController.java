package org.nastya.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.dto.CardStatusDto;
import org.nastya.demo.dto.TransferDto;
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
    public Page<CardDto> getAllCards(Pageable pageable) {
        log.info("Fetching all cards");
        return cardService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public CardDto getCardById(@PathVariable UUID id) {
        log.info("Fetching card by id={}", id);
        return cardService.getById(id);
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
    public void changeCardStatus(@Valid @RequestBody CardStatusDto cardStatusDto) {
        cardService.changeCardStatus(cardStatusDto);
    }
}
