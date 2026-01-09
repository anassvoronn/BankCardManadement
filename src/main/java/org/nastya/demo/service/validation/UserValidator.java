package org.nastya.demo.service.validation;

import lombok.RequiredArgsConstructor;
import org.nastya.demo.dto.UserDto;
import org.nastya.demo.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;

    public void validateForCreate(UserDto dto) {
        validateCommon(dto);

        if (userRepository.existsByUsername(dto.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    public void validateForUpdate(UUID userId, UserDto dto) {
        validateCommon(dto);

        userRepository.findById(userId).ifPresent(existingUser -> {
            if (!existingUser.getUsername().equals(dto.username())
                    && userRepository.existsByUsername(dto.username())) {
                throw new IllegalArgumentException("Username already exists");
            }
        });
    }

    private void validateCommon(UserDto dto) {
        if (dto.username() == null || dto.username().isBlank()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        if (dto.role() == null) {
            throw new IllegalArgumentException("User role must be specified");
        }
    }
}
