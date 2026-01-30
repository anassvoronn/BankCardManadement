package org.nastya.demo.repository;

import org.nastya.demo.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByIdAndUserId(UUID cardId, UUID userId);

    Page<Card> findAllByUserId(UUID userId, Pageable pageable);
}
