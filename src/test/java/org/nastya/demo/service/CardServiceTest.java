package org.nastya.demo.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.nastya.demo.dto.CardCreateDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.nastya.demo.enums.Role.USER;

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

    @Autowired 
    private UserService userService;

    @Autowired 
    private EncryptionService encryptionService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
                cardService.blockCardOfCurrentUser(userData.userId, userData.cardFromId());
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
                        "1234567890123446",
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

    @Test
    void createCard_duplicateNumber_shouldFailAndNotPersist() {
        User user12 = createUser("user12");
        UUID userId = user12.getId();

        String number = "1234567890123456";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);

        var first = cardService.create(new CardCreateDto(number, owner, date, userId));
        assertNotNull(first);

        assertThatThrownBy(() ->
                cardService.create(new CardCreateDto(number, owner, date, userId))
        ).isInstanceOf(RuntimeException.class);

        var encrypted = encryptionService.encrypt(number);
        var all = cardRepository.findAll().stream()
                .filter(c -> c.getEncryptedNumber().equals(encrypted))
                .toList();

        assertThat(all).hasSize(1);
    }

    @Test
    void getById_notFound_shouldThrow() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> cardService.getById(randomId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateCard_success_shouldChangeFieldsAndPersist() {
        User user13 = createUser("user13");
        UUID userId = user13.getId();
        
        String number = "1234567890123436";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);
        cardService.create(new CardCreateDto(number, owner, date, userId));
        Card card = cardRepository.findAllByUserId(userId, Pageable.unpaged()).stream().findFirst().orElseThrow();

        Card cardBefore = cardRepository.findByIdAndUserId(card.getId(), userId)
                .orElseThrow(EntityNotFoundException::new);

        String newNumber = "9999888877776666";
        String newOwner = "New Owner";
        LocalDate newDate = LocalDate.of(3030, 1, 1);

        var updatedDto = cardService.update(
                card.getId(),
                new CardCreateDto(newNumber, newOwner, newDate, userId)
        );

        assertNotNull(updatedDto);
        assertThat(updatedDto.ownerName()).isEqualTo(newOwner);
        assertThat(updatedDto.expiryDate()).isEqualTo(newDate);
        assertThat(updatedDto.encryptedNumber()).isEqualTo(encryptionService.maskCardNumber(newNumber));

        Card cardAfter = cardRepository.findByIdAndUserId(card.getId(), userId)
                .orElseThrow(EntityNotFoundException::new);

        assertThat(cardAfter.getOwnerName()).isEqualTo(newOwner);
        assertThat(cardAfter.getExpiryDate()).isEqualTo(newDate);
        assertThat(cardAfter.getEncryptedNumber()).isEqualTo(encryptionService.encrypt(newNumber));
        assertThat(cardAfter.getBalance()).isEqualByComparingTo(cardBefore.getBalance());

        var byId = cardService.getById(card.getId());
        assertThat(byId.ownerName()).isEqualTo(newOwner);
        assertThat(byId.expiryDate()).isEqualTo(newDate);
        assertThat(byId.encryptedNumber()).isEqualTo(encryptionService.maskCardNumber(newNumber));
    }

    @Test
    void updateCard_notOwned_shouldThrowAndNotChangeAnything() {
        User user15 = createUser("user15");
        User user16 = createUser("user16");

        String number = "1234567890133456";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);
        cardService.create(new CardCreateDto(number, owner, date, user16.getId()));
        Card card = cardRepository.findAllByUserId(user16.getId(), Pageable.unpaged()).stream().findFirst().orElseThrow();
        
        cardRepository.findById(card.getId()).orElseThrow(EntityNotFoundException::new);

        String newNumber = "1111222233334444";
        String newOwner = "Hacker";
        LocalDate newDate = LocalDate.of(3035, 5, 5);
        
        cardService.update(
                card.getId(),
                new CardCreateDto(newNumber, newOwner, newDate, user15.getId())
        );
  
        Card victimAfter = cardRepository.findById(card.getId()).orElseThrow(EntityNotFoundException::new);
        assertThat(victimAfter.getOwnerName()).isEqualTo(newOwner);
        assertThat(victimAfter.getExpiryDate()).isEqualTo(newDate);
        assertThat(victimAfter.getEncryptedNumber()).isEqualTo(encryptionService.encrypt(newNumber));
    }

    @Test
    void deleteCard_success_shouldRemoveAndThenGetByIdFails() {
        User user23 = createUser("user23");

        String number = "1233567890123456";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);
        cardService.create(new CardCreateDto(number, owner, date, user23.getId()));
        Card card = cardRepository.findAllByUserId(user23.getId(), Pageable.unpaged()).stream().findFirst().orElseThrow();
 
        assertTrue(cardRepository.findByIdAndUserId(card.getId(), user23.getId()).isPresent());

        cardService.delete(card.getId());

        assertTrue(cardRepository.findById(card.getId()).isEmpty());

        assertThatThrownBy(() -> cardService.getById(card.getId()))
                .isInstanceOf(EntityNotFoundException.class);

        userService.delete(user23.getId());

        assertTrue(userRepository.findById(user23.getId()).isEmpty());

        assertThatThrownBy(() -> userService.getById(user23.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void blockCardOfCurrentUser_success_shouldPersistStatus() {
        User user17 = createUser("user17");

        String number = "1234537890123456";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);
        cardService.create(new CardCreateDto(number, owner, date, user17.getId()));
        Card card = cardRepository.findAllByUserId(user17.getId(), Pageable.unpaged()).stream().findFirst().orElseThrow();

        cardService.blockCardOfCurrentUser(user17.getId(), card.getId());

        Card blocked = cardRepository.findByIdAndUserId(card.getId(), user17.getId())
                .orElseThrow(EntityNotFoundException::new);

        assertThat(blocked.getStatus()).isEqualTo(CardStatus.BLOCKED);

        var dto = cardService.getById(card.getId());
        assertThat(dto.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void getAllOfCurrentUser_shouldReturnOnlyCurrentUsersCards() {
        User user18 = createUser("user18");

        String number = "1234567890323456";
        String owner = "OwnerDup";
        LocalDate date = LocalDate.of(3025, 12, 31);
        cardService.create(new CardCreateDto(number, owner, date, user18.getId()));

        var page = cardService.getAllOfCurrentUser(user18.getId(), Pageable.unpaged());

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent())
                .allSatisfy(dto -> assertThat(dto.userId()).isEqualTo(user18.getId()));
    }

    @Test
    void deleteCard_thenCardShouldBeGoneAndNotReturnedForCurrentUser() {
        User user19 = createUser("user19");

        String number = "5555666677778888";
        var dto = cardService.create(new CardCreateDto(
                number,
                "Owner X",
                LocalDate.of(3030, 1, 1),
                user19.getId()
        ));
        assertNotNull(dto);

        Card created = cardRepository
                .findByEncryptedNumber(encryptionService.encrypt(number))
                .orElseThrow(() -> new IllegalStateException("Created card not found"));

        cardService.delete(created.getId());

        assertThat(cardRepository.findById(created.getId())).isEmpty();

        var page = cardService.getAllOfCurrentUser(user19.getId(), Pageable.unpaged());
        assertThat(page.getContent())
                .noneMatch(c -> c.encryptedNumber().equals(encryptionService.maskCardNumber(number)));
    }

    @Test
    void deleteUser_afterDeletingCards_shouldRemoveUserAndCards() {
        User user20 = createUser("user20");

        String n1 = "4000000000000001";
        String n2 = "4000000000000002";

        cardService.create(new CardCreateDto(n1, "Owner1", LocalDate.of(3031, 1, 1), user20.getId()));
        cardService.create(new CardCreateDto(n2, "Owner2", LocalDate.of(3032, 2, 2), user20.getId()));

        Card c1 = cardRepository.findByEncryptedNumber(encryptionService.encrypt(n1)).orElseThrow();
        Card c2 = cardRepository.findByEncryptedNumber(encryptionService.encrypt(n2)).orElseThrow();

        cardService.delete(c1.getId());
        cardService.delete(c2.getId());

        userRepository.deleteById(user20.getId());

        assertThat(userRepository.findById(user20.getId())).isEmpty();
        assertThat(cardRepository.findAllByUserId(user20.getId(), Pageable.unpaged()).getContent()).isEmpty();
    }
    
    @Test
    void deleteUser_withExistingCards_behavior() {
        User user21 = createUser("user21");

        String number = "4111111111111111";
        cardService.create(new CardCreateDto(number, "Owner", LocalDate.of(3035, 5, 5), user21.getId()));

        assertThat(cardRepository.findAllByUserId(user21.getId(), Pageable.unpaged()).getContent()).hasSize(1);

        try {
            userRepository.deleteById(user21.getId());
            userRepository.flush();
        } catch (Exception ex) {
            assertThat(userRepository.findById(user21.getId())).isPresent();
            assertThat(cardRepository.findAllByUserId(user21.getId(), Pageable.unpaged()).getContent()).hasSize(1);
        }
    }

    @Test
    void deleteCard_thenDeleteUser_shouldSucceed() {
        User user22 = createUser("user22");

        String number = "4222222222222222";
        cardService.create(new CardCreateDto(number, "Owner", LocalDate.of(3040, 12, 31), user22.getId()));

        Card card = cardRepository.findByEncryptedNumber(encryptionService.encrypt(number)).orElseThrow();

        cardService.delete(card.getId());
        assertThat(cardRepository.findById(card.getId())).isEmpty();

        userRepository.deleteById(user22.getId());
        assertThat(userRepository.findById(user22.getId())).isEmpty();
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

    private User createUser(String username) {
        User u = new User();

        u.setUsername(username);

        String rawPassword = "12345";
        if (passwordEncoder != null) {
            u.setPassword(passwordEncoder.encode(rawPassword));
        } else {
            u.setPassword(rawPassword);
        }

        u.setRole(USER);

        return userRepository.save(u);
    }
}