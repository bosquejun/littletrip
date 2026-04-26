package com.littletrip.api.repository;

import com.littletrip.api.model.TransitFare;
import com.littletrip.api.model.TransitFareId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransitFareRepository extends JpaRepository<TransitFare, TransitFareId> {

    @Query("SELECT f FROM TransitFare f WHERE " +
           "(f.id.stopAId = :a AND f.id.stopBId = :b) OR " +
           "(f.id.stopAId = :b AND f.id.stopBId = :a)")
    Optional<TransitFare> findByStopPair(@Param("a") UUID a, @Param("b") UUID b);

    @Query("SELECT f FROM TransitFare f WHERE f.id.stopAId = :stopId OR f.id.stopBId = :stopId")
    List<TransitFare> findAllByStopId(@Param("stopId") UUID stopId);

    @Query("SELECT f FROM TransitFare f WHERE f.id.stopAId = :stopId OR f.id.stopBId = :stopId")
    Page<TransitFare> findAllByStopId(@Param("stopId") UUID stopId, Pageable pageable);
}
