package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TransitFareDto;
import com.littletrip.api.model.TransitFare;
import com.littletrip.api.model.TransitFareId;
import com.littletrip.api.repository.TransitFareRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing transit fares between stop pairs.
 * Provides fare lookup, listing, and update operations with caching.
 */
@Slf4j
@Service
public class TransitFareService {

    private final TransitFareRepository transitFareRepository;

    public TransitFareService(TransitFareRepository transitFareRepository) {
        this.transitFareRepository = transitFareRepository;
    }

    /**
     * Lists transit fares with optional filtering by stop.
     *
     * @param stopId optional filter — if provided, returns only fares originating from this stop
     * @param page   1-based page number (converted to 0-based for Spring Data)
     * @param size   number of items per page
     * @return paginated list of transit fare DTOs
     */
    @Cacheable(value = "transit_fares", key = "'list:' + #stopId + ':' + #page + ':' + #size")
    public PagedResponse<TransitFareDto> listFares(UUID stopId, int page, int size) {
        log.info("Received request to list fares, stopId: {}, page: {}, size: {}", stopId, page, size);

        // Convert 1-based page to 0-based for Spring Data
        Pageable pageable = PageRequest.of(page-1, size);
        Page<TransitFare> farePage;

        if (stopId != null) {
            // Return only fares where the stop is the origin
            log.debug("Filtering fares by stopId: {}", stopId);
            farePage = transitFareRepository.findAllByStopId(stopId, pageable);
        } else {
            // Return all fares
            log.debug("Listing all fares");
            farePage = transitFareRepository.findAll(pageable);
        }


        log.debug("Found {} fares", farePage.getNumberOfElements());

        return new PagedResponse<>(
            farePage.getContent().stream()
            .map(TransitFareDto::from)
            .collect(Collectors.toList()),
            farePage.getNumber() + 1,
            farePage.getSize(),
            farePage.getTotalElements(),
            farePage.getTotalPages()
        );
    }

    /**
     * Looks up the fare for a specific stop pair (origin → destination).
     *
     * @param stopAId origin stop ID
     * @param stopBId destination stop ID
     * @return the transit fare if it exists
     */
    public Optional<TransitFare> getFare(UUID stopAId, UUID stopBId) {
        log.info("Received request to get fare, stopA: {}, stopB: {}", stopAId, stopBId);
        return transitFareRepository.findByStopPair(stopAId, stopBId);
    }

    /**
     * Updates the base fare for a stop pair.
     * Evicts the entire transit_fares cache after the update.
     *
     * @param stopAId the origin stop ID (part of the composite key)
     * @param stopBId the destination stop ID (part of the composite key)
     * @param request map containing the baseFareCents field to update
     * @return the updated fare if the stop pair exists
     */
    @CacheEvict(value = "transit_fares", allEntries = true)
    public Optional<TransitFare> updateFare(UUID stopAId, UUID stopBId, Map<String, Object> request) {
        log.info("Received request to update fare, stopA: {}, stopB: {}, input: {}", stopAId, stopBId, request);
        // Composite key composed of the two stop IDs
        TransitFareId id = new TransitFareId(stopAId, stopBId);
        return transitFareRepository.findById(id).map(existing -> {
            log.debug("Found existing fare record");
            if (request.containsKey("baseFareCents")) {
                // Handle both Integer and Long numeric types from JSON parsing
                existing.setBaseFareCents(((Number) request.get("baseFareCents")).intValue());
            }
            TransitFare saved = transitFareRepository.save(existing);
            log.debug("Updated fare, stopA: {}, stopB: {}", stopAId, stopBId);
            return saved;
        });
    }

    /**
     * Gets the fare in cents for a stop pair.
     * Cached using a key composed of both stop IDs.
     *
     * @param fromStopId origin stop ID
     * @param toStopId   destination stop ID
     * @return the fare in cents, or 0 if no fare exists for the pair
     */
    @Cacheable(value = "transit_fares", key = "#fromStopId + ':' + #toStopId")
    public int getFareCents(UUID fromStopId, UUID toStopId) {
        log.info("Getting fare in cents, from: {}, to: {}", fromStopId, toStopId);
        return transitFareRepository.findByStopPair(fromStopId, toStopId)
                .map(TransitFare::getBaseFareCents)
                .orElse(0);
    }

    /**
     * Gets the maximum fare for a given stop (the highest fare from that stop to any destination).
     * Used when a journey is marked incomplete and the maximum possible fare is charged.
     *
     * @param stopId the origin stop ID
     * @return the maximum fare in cents, or 0 if no fares exist
     */
    @Cacheable(value = "transit_fares", key = "'max:' + #stopId")
    public int getMaxFareFrom(UUID stopId) {
        log.info("Getting max fare from stop: {}", stopId);
        return transitFareRepository.findAllByStopId(stopId)
                .stream()
                .mapToInt(TransitFare::getBaseFareCents)
                .max()
                .orElse(0);
    }
}
