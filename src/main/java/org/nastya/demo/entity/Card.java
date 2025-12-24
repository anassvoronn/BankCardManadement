package org.nastya.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.nastya.demo.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Data
public class Card {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String encryptedNumber;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

