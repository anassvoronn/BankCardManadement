package org.nastya.demo.service;

import org.nastya.demo.dto.CardDto;
import org.nastya.demo.entity.Card;

public interface CardMapper {

    CardDto toDto(Card card);

    Card toEntity(CardDto dto);
}
