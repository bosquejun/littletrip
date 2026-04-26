package com.littletrip.api.repository;

import com.littletrip.api.model.Journey;
import com.littletrip.api.model.JourneyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    Optional<Journey> findByCardTokenAndStatus(String cardToken, JourneyStatus status);

    List<Journey> findByCardTokenOrderByStartedAtDesc(String cardToken);

    Page<Journey> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Journey> findByCardTokenOrderByCreatedAtDesc(String cardToken, Pageable pageable);

    List<Journey> findByOperatorIdOrderByCreatedAtDesc(UUID operatorId);

    Page<Journey> findByOperatorIdOrderByCreatedAtDesc(UUID operatorId, Pageable pageable);

    Page<Journey> findByCardTokenAndOperatorIdOrderByCreatedAtDesc(String cardToken, UUID operatorId, Pageable pageable);
}