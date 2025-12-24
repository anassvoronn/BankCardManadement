package org.nastya.demo.service;

import org.nastya.demo.dto.UserDto;
import org.nastya.demo.entity.User;

public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(UserDto dto);
}
