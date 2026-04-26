package com.littletrip.api.repository;

import com.littletrip.api.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT ak FROM ApiKey ak WHERE ak.keyHash = :keyHash AND ak.active = true")
    Optional<ApiKey> findActiveByKeyHash(@Param("keyHash") String keyHash);
}