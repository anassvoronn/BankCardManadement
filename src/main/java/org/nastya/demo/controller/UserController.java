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
import org.nastya.demo.dto.UserDto;
import org.nastya.demo.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Controller", description = "Управление пользователями")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей получен")
    @GetMapping
    public Page<UserDto> getAllUsers(@ParameterObject Pageable pageable) {
        log.info("Fetching all users");
        return userService.getAll(pageable);
    }

    @Operation(summary = "Получить пользователя по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{id}")
    public UserDto getUserById(
            @Parameter(description = "ID пользователя") @PathVariable UUID id
    ) {
        log.info("Fetching user by id={}", id);
        return userService.getById(id);
    }

    @Operation(summary = "Создать пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные")
    })
    @PostMapping
    public UUID createUser(
            @RequestBody @Valid UserDto dto
    ) {
        log.info("Creating user with username={}", dto.username());
        return userService.create(dto);
    }

    @Operation(summary = "Обновить пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PutMapping("/{id}")
    public UserDto updateUser(
            @Parameter(description = "ID пользователя") @PathVariable UUID id,
            @RequestBody @Valid UserDto dto
    ) {
        log.info("Updating user id={}", id);
        return userService.update(id, dto);
    }

    @Operation(summary = "Удалить пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь удалён")
    @DeleteMapping("/{id}")
    public void deleteUser(
            @Parameter(description = "ID пользователя") @PathVariable UUID id
    ) {
        log.info("Deleting user id={}", id);
        userService.delete(id);
    }
}