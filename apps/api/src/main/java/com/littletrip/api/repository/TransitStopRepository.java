package com.littletrip.api.repository;

import com.littletrip.api.model.TransitStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransitStopRepository extends JpaRepository<TransitStop, UUID> {

    Optional<TransitStop> findByName(String name);
}