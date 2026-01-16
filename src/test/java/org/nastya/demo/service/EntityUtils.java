package org.nastya.demo.service;

import org.nastya.demo.entity.Card;
import org.nastya.demo.entity.User;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EntityUtils {

    public static final String PASSWORD = "12345";

    public static Card createCard(
            User user,
            String encryptedNumber,
            BigDecimal balance
    ) {
        Card card = new Card();
        card.setEncryptedNumber(encryptedNumber);
        card.setOwnerName("Alice");
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        card.setUser(user);

        return card;
    }

    public static User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(PASSWORD);
        user.setRole(Role.USER);

        return user;
    }
}
