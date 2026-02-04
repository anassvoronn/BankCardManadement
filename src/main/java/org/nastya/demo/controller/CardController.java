package org.nastya.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Card Controller", description = "Управление банковскими картами")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Получить все карты")
    @ApiResponse(responseCode = "200", description = "Список карт успешно получен")
    @GetMapping
    public Page<CardDto> getAll(Pageable pageable) {
        log.info("fetching all cards");
        return cardService.getAll(pageable);
    }

    @Operation(summary = "Получить карту по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта найдена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @GetMapping("/{id}")
    public CardDto getById(
            @Parameter(description = "ID карты") @PathVariable UUID id
    ) {
        log.info("fetching card by id={}", id);
        return cardService.getById(id);
    }

    @Operation(summary = "Получить все карты текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Карты пользователя успешно получены")
    @GetMapping("/private")
    public Page<CardDto> getAllOfCurrentUser(Pageable pageable) {
        log.info("fetching own cards");
        return cardService.getAllOfCurrentUser(pageable);
    }

    @Operation(summary = "Получить карту текущего пользователя по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта найдена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @GetMapping("/private/{id}")
    public CardDto getByIdOfCurrentUser(
            @Parameter(description = "ID карты") @PathVariable UUID id
    ) {
        log.info("fetching own card by id={}", id);
        return cardService.getByIdOfCurrentUser(id);
    }

    @Operation(summary = "Перевод между своими картами")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Перевод выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    })
    @PutMapping("/private/transfer")
    public void transferBetweenOwnCards(
            @RequestBody @Valid TransferDto dto
    ) {
        cardService.transferBetweenOwnCards(dto);
    }

    @Operation(summary = "Заблокировать карту текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Карта заблокирована")
    @PutMapping("/private/{id}/block")
    public void blockCardOfCurrentUser(
            @Parameter(description = "ID карты") @PathVariable UUID id
    ) {
        cardService.blockCardOfCurrentUser(id);
    }

    @Operation(summary = "Получить баланс карты текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Баланс успешно получен")
    @GetMapping("/private/{id}/balance")
    public BigDecimal getCardBalanceOfCurrentUser(
            @Parameter(description = "ID карты") @PathVariable UUID id
    ) {
        return cardService.getCardBalanceOfCurrentUser(id);
    }

    @Operation(summary = "Создать карту")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные")
    })
    @PostMapping
    public CardDto createCard(
            @RequestBody @Valid CardCreateDto dto
    ) {
        log.info("Creating card for userId={}", dto.userId());
        return cardService.create(dto);
    }

    @Operation(summary = "Обновить карту")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта обновлена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PutMapping("/{id}")
    public CardDto updateCard(
            @Parameter(description = "ID карты") @PathVariable UUID id,
            @RequestBody @Valid CardCreateDto dto
    ) {
        log.info("Updating card id={}", id);
        return cardService.update(id, dto);
    }

    @Operation(summary = "Удалить карту")
    @ApiResponse(responseCode = "200", description = "Карта удалена")
    @DeleteMapping("/{id}")
    public void deleteCard(
            @Parameter(description = "ID карты") @PathVariable UUID id
    ) {
        log.info("Deleting card id={}", id);
        cardService.delete(id);
    }

    @Operation(summary = "Изменить статус карты")
    @ApiResponse(responseCode = "200", description = "Статус карты изменён")
    @PutMapping("/{id}/status")
    public void changeCardStatus(
            @Parameter(description = "ID карты") @PathVariable UUID id,
            @Parameter(description = "Новый статус карты") @RequestParam CardStatus status
    ) {
        cardService.changeCardStatus(id, status);
    }
}
