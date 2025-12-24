package org.nastya.demo.service.mapper;

import lombok.NonNull;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.entity.User;
import org.nastya.demo.service.CardMapper;
import org.springframework.stereotype.Component;

@Component
public class CardMapperImpl implements CardMapper {

    @Override
    public CardDto toDto(@NonNull Card card) {
        return new CardDto(
                card.getEncryptedNumber(),
                card.getOwnerName(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser() != null ? card.getUser().getId() : null
        );
    }

    @Override
    public Card toEntity(@NonNull CardDto dto) {
        Card card = new Card();
        card.setEncryptedNumber(dto.encryptedNumber());
        card.setOwnerName(dto.ownerName());
        card.setExpiryDate(dto.expiryDate());
        card.setStatus(dto.status());
        card.setBalance(dto.balance());

        if (dto.userId() != null) {
            User user = new User();
            user.setId(dto.userId());
            card.setUser(user);
        }

        return card;
    }
}
