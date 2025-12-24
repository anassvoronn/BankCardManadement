package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nastya.demo.dto.UserDto;
import org.nastya.demo.entity.User;
import org.nastya.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto getById(UUID id) {
        log.info("Fetching user by id={}", id);

        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("User not found, id={}", id);
                    return new EntityNotFoundException("User not found");
                });
    }

    public List<UserDto> getAll() {
        log.info("Fetching all users");

        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UUID create(UserDto dto) {
        log.info("Creating user with username={}", dto.username());

        validateUser(dto);

        if (userRepository.existsByUsername(dto.username())) {
            log.warn("Username already exists: {}", dto.username());
            throw new IllegalArgumentException("Username already exists");
        }

        User user = userMapper.toEntity(dto);
        User saved = userRepository.save(user);

        log.info("User created successfully, id={}", saved.getId());
        return saved.getId();
    }

    public UserDto update(UUID id, UserDto dto) {
        log.info("Updating user id={}", id);

        validateUser(dto);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for update, id={}", id);
                    return new EntityNotFoundException("User not found");
                });

        if (!user.getUsername().equals(dto.username())
                && userRepository.existsByUsername(dto.username())) {
            log.warn("Username already exists: {}", dto.username());
            throw new IllegalArgumentException("Username already exists");
        }

        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setRole(dto.role());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully, id={}", id);
        return userMapper.toDto(updatedUser);
    }

    public void delete(UUID id) {
        log.info("Deleting user id={}", id);

        if (!userRepository.existsById(id)) {
            log.warn("User not found for delete, id={}", id);
            throw new EntityNotFoundException("User not found");
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully, id={}", id);
    }

    private void validateUser(UserDto dto) {
        if (dto.username() == null || dto.username().isBlank()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        if (dto.role() == null) {
            throw new IllegalArgumentException("User role must be specified");
        }
    }
}
