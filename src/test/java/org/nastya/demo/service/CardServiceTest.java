package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.nastya.demo.dto.CardCreateDto;
import org.nastya.demo.dto.TransferDto;
import org.nastya.demo.entity.Card;
import org.nastya.demo.enums.CardStatus;
import org.nastya.demo.repository.CardRepository;
import org.nastya.demo.repository.UserRepository;
import org.nastya.demo.service.validation.CardValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @MockitoSpyBean
    private CustomUserDetailsService customUserDetailsService;

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Test
    void transferBetweenOwnCards_success() {
        UserData userData = getUserData("user5");
        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), amount);

        cardService.transferBetweenOwnCards(dto);

        Card from = cardRepository.findById(userData.cardFromId()).orElseThrow(EntityNotFoundException::new);
        Card to = cardRepository.findById(userData.cardToId()).orElseThrow(EntityNotFoundException::new);

        assertThat(from.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(to.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1400));
        assertEquals(1L, from.getVersion());
        assertEquals(1L, to.getVersion());
    }

    @Test
    void transferBetweenOwnCards_insufficientBalance() {
        UserData userData = getUserData("user3");
        BigDecimal amount = BigDecimal.valueOf(2000);
        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), amount);

        assertThatThrownBy(() -> cardService.transferBetweenOwnCards(dto)).
                isInstanceOf(IllegalStateException.class).
                hasMessageContaining("Insufficient");
        Card from = cardRepository.findById(userData.cardFromId()).orElseThrow(EntityNotFoundException::new);
        Card to = cardRepository.findById(userData.cardToId()).orElseThrow(EntityNotFoundException::new);

        assertEquals(0L, from.getVersion());
        assertEquals(0L, to.getVersion());
    }

    @Test
    void transferBetweenOwnCards() {
        UserData userData = getUserData("user4");
        doAnswer(invocation -> {
            Thread blockingCardTread = new Thread(() -> {
                cardService.blockCardOfCurrentUser(userData.cardFromId());
            });
            blockingCardTread.start();
            blockingCardTread.join();
            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), amount);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );

        Optional<Card> from = cardRepository.findByIdAndUserId(userData.cardFromId(), userData.userId());
        Optional<Card> to = cardRepository.findByIdAndUserId(userData.cardToId(),  userData.userId());

        assertEquals(
                0,
                from.get().getBalance().compareTo(BigDecimal.valueOf(1000))
        );

        assertEquals(
                0,
                to.get().getBalance().compareTo(BigDecimal.valueOf(1200))
        );


        assertEquals(CardStatus.BLOCKED, from.get().getStatus());
        assertEquals(CardStatus.ACTIVE, to.get().getStatus());

        assertEquals(1L, from.get().getVersion());
        assertEquals(0L, to.get().getVersion());
    }

    @Test
    void twoTransferBetweenOwnCards() {
        UserData userData = getUserData("user7");
        AtomicBoolean callingValidator = new AtomicBoolean(false);

        doAnswer(invocation -> {

            if (callingValidator.compareAndSet(false, true)) {

                Thread transferThread = new Thread(() -> {
                    BigDecimal amount = BigDecimal.valueOf(1000);
                    TransferDto dto =
                            new TransferDto(userData.cardFromId(), userData.cardToId(), amount);

                    cardService.transferBetweenOwnCards(dto);
                });

                transferThread.start();
                transferThread.join();
            }

            return invocation.callRealMethod();

        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal amount = BigDecimal.valueOf(200);

        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), amount);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );

        Optional<Card> from = cardRepository.findByIdAndUserId(userData.cardFromId(), userData.userId());
        Optional<Card> to = cardRepository.findByIdAndUserId( userData.cardToId(), userData.userId());

        assertEquals(
                0,
                from.get().getBalance().compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                0,
                to.get().getBalance().compareTo(BigDecimal.valueOf(2200))
        );
        assertEquals(1L, from.get().getVersion());
        assertEquals(1L, to.get().getVersion());
    }

    @Test
    void updateAndTransferBetweenOwnCards() {
        UserData userData = getUserData("user8");
        doAnswer(invocation -> {
            Thread updateCardThread = new Thread(() -> {
                CardCreateDto dto = new CardCreateDto(
                        "1234567890123456",
                        "Vika",
                        LocalDate.of(2030, 1, 1),
                        userData.userId()
                );
                cardService.update(userData.cardFromId(), dto);
            });
            updateCardThread.start();
            updateCardThread.join();

            return invocation.callRealMethod();
        }).when(cardValidator)
                .validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal fromBalanceBefore =
                cardRepository.findByIdAndUserId(userData.cardFromId(), userData.userId()).get().getBalance();
        BigDecimal toBalanceBefore =
                cardRepository.findByIdAndUserId(userData.cardToId(), userData.userId()).get().getBalance();

        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), BigDecimal.valueOf(200));

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> cardService.transferBetweenOwnCards(dto)
        );

        Card from = cardRepository.findByIdAndUserId(userData.cardFromId(), userData.userId()).get();
        Card to = cardRepository.findByIdAndUserId(userData.cardToId(), userData.userId()).get();

        assertEquals(0, from.getBalance().compareTo(fromBalanceBefore));
        assertEquals(0, to.getBalance().compareTo(toBalanceBefore));

        assertEquals("Vika", from.getOwnerName());
        assertEquals(LocalDate.of(2030, 1, 1), from.getExpiryDate());
        assertEquals(1L, from.getVersion());
        assertEquals(0L, to.getVersion());
    }

    @Test
    void deleteAndTransferBetweenOwnCards() {
        UserData userData = getUserData("user6");
        doAnswer(invocation -> {
            Thread deleteCardThread = new Thread(() -> cardService.delete(userData.cardFromId()));
            deleteCardThread.start();
            deleteCardThread.join();
            return invocation.callRealMethod();
        }).when(cardValidator).validateCardsAreActive(any(Card.class), any(Card.class));

        BigDecimal toBalanceBefore = cardRepository.findByIdAndUserId(userData.cardToId(), userData.userId()).get().getBalance();

        TransferDto dto = new TransferDto(userData.cardFromId(), userData.cardToId(), BigDecimal.valueOf(200));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> cardService.transferBetweenOwnCards(dto));

        Optional<Card> from = cardRepository.findByIdAndUserId(userData.cardFromId(), userData.userId());
        Optional<Card> to = cardRepository.findByIdAndUserId(userData.cardToId(), userData.userId());

        assertTrue(from.isEmpty(), "Source card should be deleted");

        assertEquals(0, to.get().getBalance().compareTo(toBalanceBefore), "Target card balance should remain unchanged");
        assertEquals(0L, to.get().getVersion());
    }

    private UserData getUserData(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User " + username + " not found"));

        doReturn(user).when(customUserDetailsService).getCurrentUser();

        List<Card> cards = cardRepository.findAllByUserId(user.getId(), Pageable.unpaged()).getContent();
        if (cards.size() < 2)
            throw new IllegalStateException("Liquibase must create at least 2 cards for user " + username);

        return new UserData(user.getId(), cards.get(0).getId(), cards.get(1).getId());
    }

    private record UserData(UUID userId,
                            UUID cardFromId,
                            UUID cardToId) {
    }
}