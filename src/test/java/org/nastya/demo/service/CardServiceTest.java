package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nastya.demo.dto.CardStatusDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.entity.User;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.repository.CardRepository;
import org.nastya.demo.repository.UserRepository;
import org.nastya.demo.service.validation.CardValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.nastya.demo.service.EntityUtils.createCard;
import static org.nastya.demo.service.EntityUtils.createUser;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private CardRepository cardRepository;

    @MockitoSpyBean
    private CardValidator cardValidator;

    @Autowired
    private ApplicationContext applicationContext;

    private UUID userId;
    private UUID cardFromId;
    private UUID cardToId;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        User user = createUser("alice");
        user = userRepository.save(user);

        userId = user.getId();

        Card from = createCard(
                user,
                "d05daad5efab9f40373287e3f411c6eb",
                BigDecimal.valueOf(1000)
        );

        Card to = createCard(
                user,
                "8169afdfcef2f83e20eac7479aa1aa16",
                BigDecimal.valueOf(500)
        );

        from = cardRepository.save(from);
        to = cardRepository.save(to);

        cardFromId = from.getId();
        cardToId = to.getId();
    }

    @Test
    void transferBetweenOwnCards_success() {
        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);

        cardService.transferBetweenOwnCards(dto);

        Card fromCard = cardRepository.findById(cardFromId).orElseThrow(EntityNotFoundException::new);
        Card toCard = cardRepository.findById(cardToId).orElseThrow(EntityNotFoundException::new);

        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(toCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void transferBetweenOwnCards_insufficientBalance() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);

        assertThatThrownBy(() -> cardService.transferBetweenOwnCards(dto)).
                isInstanceOf(IllegalStateException.class).
                hasMessageContaining("Insufficient");
    }

    @Test
    void transferBetweenOwnCards() throws InterruptedException {
        Semaphore transferSemaphore = new Semaphore(0);
        Semaphore blockSemaphore = new Semaphore(0);

        doAnswer(invocation -> {
            transferSemaphore.release();
            blockSemaphore.acquire();

            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        Thread transferThread = new Thread(() -> {
            BigDecimal amount = BigDecimal.valueOf(200);

            TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);

            Assertions.assertThrows(
                    OptimisticLockException.class,
                    () -> cardService.transferBetweenOwnCards(dto)
            );
        });

        Thread blockingCardTread = new Thread(() -> {
            try {
                transferSemaphore.acquire();
                CardStatusDto cardStatusDto = new CardStatusDto(cardFromId, userId, CardStatus.BLOCKED);
                cardService.changeCardStatus(cardStatusDto);
                blockSemaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            blockSemaphore.release();

        });
        transferThread.start();
        blockingCardTread.start();

        transferThread.join();
        blockingCardTread.join();
    }
}