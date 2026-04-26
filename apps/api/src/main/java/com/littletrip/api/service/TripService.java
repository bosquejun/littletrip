package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TripDto;
import com.littletrip.api.model.Journey;
import com.littletrip.api.model.JourneyStatus;
import com.littletrip.api.model.TapEvent;
import com.littletrip.api.model.TapType;
import com.littletrip.api.model.TransitStop;
import com.littletrip.api.repository.JourneyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing transit journeys (trips).
 * Handles the state machine logic for ON/OFF tap events and fare calculation.
 */
@Slf4j
@Service
public class TripService {

    private final JourneyRepository journeyRepository;
    private final TransitFareService transitFareService;

    public TripService(JourneyRepository journeyRepository, TransitFareService transitFareService) {
        this.journeyRepository = journeyRepository;
        this.transitFareService = transitFareService;
    }

    /**
     * Processes a tap event to create or complete a journey.
     * This method implements the transit tap state machine:
     *
     * <ul>
     *   <li><b>ON tap:</b> If there is an incomplete journey for the card, close it
     *       (mark INCOMPLETE or CANCELLED). Then start a new journey.</li>
     *   <li><b>OFF tap:</b> If there is an incomplete journey, complete it by setting
     *       the destination and calculating the fare. If no incomplete journey exists,
     *       return null.</li>
     * </ul>
     *
     * Journeys can have the following statuses:
     * <ul>
     *   <li>IN_PROGRESS — journey started but not yet completed</li>
     *   <li>INCOMPLETE — closed without a matching OFF tap (charged max fare)</li>
     *   <li>CANCELLED — ON tap and OFF tap at the same stop (no charge)</li>
     *   <li>COMPLETED — normal journey with OFF tap at a different stop</li>
     * </ul>
     *
     * @param tapEvent the tap event to process
     * @return the journey created or updated, or null if an OFF tap had no matching ON tap
     */
    @Transactional
    @CacheEvict(value = "trips", allEntries = true)
    public Journey processTap(TapEvent tapEvent) {
        String cardToken = tapEvent.getCardToken();
        log.info("Processing tap for trip logic, cardToken: {}, tapType: {}", cardToken, tapEvent.getTapType());

        // Look for any existing incomplete journey for this card
        Optional<Journey> existingIncomplete = journeyRepository.findByCardTokenAndStatus(cardToken, JourneyStatus.IN_PROGRESS);

        if (tapEvent.getTapType() == TapType.ON) {
            log.debug("Tap type is ON, checking for existing incomplete journey");
            if (existingIncomplete.isPresent()) {
                Journey journey = existingIncomplete.get();
                log.debug("Found existing incomplete journey, id: {}, closing it", journey.getId());

                // Close the previous journey
                journey.setEndedAt(tapEvent.getCreatedAt());
                journey.setStatus(JourneyStatus.INCOMPLETE);

                UUID existingOriginId = journey.getOriginStop().getId();
                UUID newStopId = tapEvent.getStop().getId();

                if (existingOriginId.equals(newStopId)) {
                    // Tapping on at the same stop as the origin = cancelled trip
                    log.debug("New tap same as origin stop, cancelling journey");
                    journey.setStatus(JourneyStatus.CANCELLED);
                    journey.setFareAmount(0);
                } else {
                    // Charge the maximum possible fare for an incomplete journey
                    int maxFare = transitFareService.getMaxFareFrom(existingOriginId);
                    log.debug("New tap different stop, setting max fare: {} cents", maxFare);
                    journey.setFareAmount(maxFare);
                }
                journeyRepository.save(journey);
                log.info("Closed incomplete journey, id: {}, status: {}", journey.getId(), journey.getStatus());
            }

            // Start a new journey from the current stop
            log.debug("Creating new journey for cardToken: {}", cardToken);
            Journey newJourney = new Journey();
            newJourney.setCardToken(cardToken);
            newJourney.setOperator(tapEvent.getOperator());
            newJourney.setDevice(tapEvent.getDevice());
            newJourney.setOriginStop(tapEvent.getStop());
            newJourney.setStartedAt(tapEvent.getCreatedAt());
            newJourney.setStatus(JourneyStatus.IN_PROGRESS);
            newJourney.setFareAmount(0);
            newJourney.setRequestId(tapEvent.getRequestId());
            Journey saved = journeyRepository.save(newJourney);
            log.info("Created new journey, id: {}", saved.getId());
            return saved;

        } else {
            // OFF tap — attempt to complete the existing journey
            log.debug("Tap type is OFF, attempting to complete journey");
            if (existingIncomplete.isPresent()) {
                Journey journey = existingIncomplete.get();
                TransitStop originStop = journey.getOriginStop();
                TransitStop destinationStop = tapEvent.getStop();

                log.debug("Found journey to complete, origin: {}, destination: {}", originStop.getName(), destinationStop.getName());
                journey.setDestinationStop(destinationStop);
                journey.setEndedAt(tapEvent.getCreatedAt());

                if (originStop.getId().equals(destinationStop.getId())) {
                    // OFF tap at the same stop as the ON tap = cancelled
                    log.debug("Origin equals destination, cancelling journey");
                    journey.setStatus(JourneyStatus.CANCELLED);
                    journey.setFareAmount(0);
                } else {
                    // Calculate the fare for the actual journey
                    int fare = transitFareService.getFareCents(originStop.getId(), destinationStop.getId());
                    log.debug("Calculated fare: {} cents", fare);
                    journey.setStatus(JourneyStatus.COMPLETED);
                    journey.setFareAmount(fare);
                }
                Journey saved = journeyRepository.save(journey);
                log.info("Completed journey, id: {}, status: {}, fare: {}", saved.getId(), saved.getStatus(), saved.getFareAmount());
                return saved;

            } else {
                // OFF tap with no matching ON tap — ignored
                log.warn("No incomplete journey found for cardToken: {}, returning null", cardToken);
                return null;
            }
        }
    }

    /**
     * Retrieves all trips ordered by creation date descending.
     * Cached per page/size combination.
     *
     * @param page 1-based page number
     * @param size number of items per page
     * @return paginated list of trip DTOs
     */
    @Cacheable(value = "trips", key = "'all:' + #page + ':' + #size")
    public PagedResponse<TripDto> getAllTrips(int page, int size) {
        log.info("Received request to list all trips, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page-1, size);
        Page<Journey> journeyPage = journeyRepository.findAllByOrderByCreatedAtDesc(pageable);
        log.debug("Found {} trips", journeyPage.getNumberOfElements());
        return toPagedResponse(journeyPage);
    }

    /**
     * Retrieves trips for a specific card token ordered by creation date descending.
     * Cached per cardToken + page + size combination.
     *
     * @param cardToken the card token to filter by
     * @param page      1-based page number
     * @param size      number of items per page
     * @return paginated list of trip DTOs
     */
    @Cacheable(value = "trips", key = "'cardToken:' + #cardToken + ':' + #page + ':' + #size")
    public PagedResponse<TripDto> getTripsByCardToken(String cardToken, int page, int size) {
        log.info("Received request to list trips by cardToken: {}, page: {}, size: {}", cardToken, page, size);
        Pageable pageable = PageRequest.of(page-1, size);
        Page<Journey> journeyPage = journeyRepository.findByCardTokenOrderByCreatedAtDesc(cardToken, pageable);
        log.debug("Found {} trips for cardToken: {}", journeyPage.getNumberOfElements(), cardToken);
        return toPagedResponse(journeyPage);
    }

    /**
     * Retrieves a single trip by its ID.
     * Cached based on the journey UUID.
     *
     * @param id the journey UUID
     * @return the trip DTO if found, null otherwise
     */
    @Cacheable(value = "trips", key = "'id:' + #id.toString()")
    public TripDto getTripById(UUID id) {
        log.info("Received request to get trip by id: {}", id);
        return journeyRepository.findById(id)
            .map(TripDto::from)
            .orElse(null);
    }

    /**
     * Lists trips with flexible filtering by cardToken and/or operatorId.
     *
     * @param cardToken   optional filter by card token
     * @param operatorId optional filter by operator
     * @param page       1-based page number
     * @param size       number of items per page
     * @return paginated list of trip DTOs
     */
    public PagedResponse<TripDto> getTrips(String cardToken, UUID operatorId, int page, int size) {
        log.info("Received request to list trips, cardToken: {}, operatorId: {}, page: {}, size: {}",
            cardToken, operatorId, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Journey> journeyPage;

        // Choose the most specific query available based on which filters are provided.
        // More filters = narrower query = better DB performance.
        if (cardToken != null && operatorId != null) {
            journeyPage = journeyRepository.findByCardTokenAndOperatorIdOrderByCreatedAtDesc(cardToken, operatorId, pageable);
            log.debug("Querying by cardToken + operatorId");
        } else if (cardToken != null) {
            journeyPage = journeyRepository.findByCardTokenOrderByCreatedAtDesc(cardToken, pageable);
            log.debug("Querying by cardToken only");
        } else if (operatorId != null) {
            journeyPage = journeyRepository.findByOperatorIdOrderByCreatedAtDesc(operatorId, pageable);
            log.debug("Querying by operatorId only");
        } else {
            journeyPage = journeyRepository.findAllByOrderByCreatedAtDesc(pageable);
            log.debug("Querying all trips");
        }

        log.debug("Found {} trips", journeyPage.getNumberOfElements());
        return toPagedResponse(journeyPage);
    }

    /**
     * Converts a Spring Data Page of Journey entities to a PagedResponse of TripDtos.
     *
     * @param journeyPage the Spring Data page
     * @return a PagedResponse with TripDto content
     */
    private PagedResponse<TripDto> toPagedResponse(Page<Journey> journeyPage) {
        List<TripDto> content = journeyPage.getContent().stream()
            .map(TripDto::from)
            .collect(Collectors.toList());

        return new PagedResponse<>(
            content,
            journeyPage.getNumber() + 1,
            journeyPage.getSize(),
            journeyPage.getTotalElements(),
            journeyPage.getTotalPages()
        );
    }
}
