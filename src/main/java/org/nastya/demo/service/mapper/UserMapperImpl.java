package org.nastya.demo.service.mapper;

import lombok.NonNull;
import org.nastya.demo.dto.UserDto;
import org.nastya.demo.entity.User;
import org.nastya.demo.service.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(@NonNull User user) {
        return new UserDto(
                user.getUsername(),
                user.getPassword(),
                user.getRole()
        );
    }

    @Override
    public User toEntity(@NonNull UserDto dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setRole(dto.role());
        return user;
    }
}
