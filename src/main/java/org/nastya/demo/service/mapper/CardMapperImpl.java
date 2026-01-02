package org.nastya.demo.service.mapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.nastya.demo.dto.CardDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.service.CardMapper;
import org.nastya.demo.service.EncryptionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMapperImpl implements CardMapper {
    private final EncryptionService encryptionService;

    @Override
    public CardDto toDto(@NonNull Card card) {
        String plain = encryptionService.decrypt(card.getEncryptedNumber());
        String masked = encryptionService.maskCardNumber(plain);

        return new CardDto(
                masked,
                card.getOwnerName(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId()
        );
    }
}
