package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nastya.demo.dto.CardCreateDto;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.nastya.demo.service.EntityUtils.createCard;
import static org.nastya.demo.service.EntityUtils.createUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @MockitoSpyBean
    private CardValidator cardValidator;

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
                "sozVmiVDbDqO/OO3YAPHa4qjYkH96N8FTcMlxsaVuJ4=", //1234567890123456
                BigDecimal.valueOf(1000)
        );

        Card to = createCard(
                user,
                "5suMnEocPp5oJHfhy98yeYqjYkH96N8FTcMlxsaVuJ4=", // 0123456789123456
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
        doAnswer(invocation -> {
            Thread blockingCardTread = new Thread(() -> {
                CardStatusDto cardStatusDto = new CardStatusDto(cardFromId, userId, CardStatus.BLOCKED);
                cardService.changeCardStatus(cardStatusDto);
            });
            blockingCardTread.start();
            blockingCardTread.join();
            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );

        Optional<Card> from = cardRepository.findByIdAndUserId(cardFromId, userId);
        Optional<Card> to = cardRepository.findByIdAndUserId(cardToId, userId);

        assertEquals(
                0,
                from.get().getBalance().compareTo(BigDecimal.valueOf(1000))
        );

        assertEquals(
                0,
                to.get().getBalance().compareTo(BigDecimal.valueOf(500))
        );


        assertEquals(CardStatus.BLOCKED, from.get().getStatus());
        assertEquals(CardStatus.ACTIVE, to.get().getStatus());

    }

    @Test
    void twoTransferBetweenOwnCards() throws InterruptedException {
        doAnswer(invocation -> {
            Thread transferThread = new Thread(() -> {
                BigDecimal amount = BigDecimal.valueOf(1000);

                TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);
                cardService.transferBetweenOwnCards(dto);
            });

            transferThread.start();
            transferThread.join();
            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, amount);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );


        Optional<Card> from = cardRepository.findByIdAndUserId(cardFromId, userId);
        Optional<Card> to = cardRepository.findByIdAndUserId(cardToId, userId);

        assertEquals(
                0,
                from.get().getBalance().compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                0,
                to.get().getBalance().compareTo(BigDecimal.valueOf(1500))
        );
    }

    @Test
    void updateAndTransferBetweenOwnCards() throws InterruptedException {
        doAnswer(invocation -> {
            Thread updateCardThread = new Thread(() -> {
                CardCreateDto dto = new CardCreateDto(
                        "1234567890123456",
                        "Vika",
                        LocalDate.of(2030, 1, 1),
                        userId
                );
                cardService.update(cardFromId, dto);
            });
            updateCardThread.start();
            updateCardThread.join();

            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal fromBalanceBefore =
                cardRepository.findByIdAndUserId(cardFromId, userId).get().getBalance();
        BigDecimal toBalanceBefore =
                cardRepository.findByIdAndUserId(cardToId, userId).get().getBalance();

        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, BigDecimal.valueOf(200));

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );

        Card from = cardRepository.findByIdAndUserId(cardFromId, userId).get();
        Card to = cardRepository.findByIdAndUserId(cardToId, userId).get();

        assertEquals(0, from.getBalance().compareTo(fromBalanceBefore));
        assertEquals(0, to.getBalance().compareTo(toBalanceBefore));

        assertEquals("Vika", from.getOwnerName());
        assertEquals(LocalDate.of(2030, 1, 1), from.getExpiryDate());
    }

    @Test
    void deleteAndTransferBetweenOwnCards() throws InterruptedException {
        doAnswer(invocation -> {
            Thread deleteCardThread = new Thread(() -> cardService.delete(cardFromId));
            deleteCardThread.start();
            deleteCardThread.join();
            return invocation.callRealMethod();
        }).when(cardValidator).validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal toBalanceBefore = cardRepository.findByIdAndUserId(cardToId, userId).get().getBalance();

        TransferDto dto = new TransferDto(userId, cardFromId, cardToId, BigDecimal.valueOf(200));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> cardService.transferBetweenOwnCards(dto));

        Optional<Card> from = cardRepository.findByIdAndUserId(cardFromId, userId);
        Optional<Card> to = cardRepository.findByIdAndUserId(cardToId, userId);

        assertTrue(from.isEmpty(), "Source card should be deleted");

        assertEquals(0, to.get().getBalance().compareTo(toBalanceBefore), "Target card balance should remain unchanged");
    }
}