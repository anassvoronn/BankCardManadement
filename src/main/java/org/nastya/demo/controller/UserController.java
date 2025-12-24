package org.nastya.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.UserDto;
import org.nastya.demo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> getAllUsers() {
        log.info("Fetching all users");
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable UUID id) {
        log.info("Fetching user by id={}", id);
        return userService.getById(id);
    }

    @PostMapping
    public UUID createUser(@Valid @RequestBody UserDto dto) {
        log.info("Creating user with username={}", dto.username());
        return userService.create(dto);
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto dto) {
        log.info("Updating user id={}", id);
        return userService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        log.info("Deleting user id={}", id);
        userService.delete(id);
    }
}
