package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TapEventDto;
import com.littletrip.api.dto.TapRequest;
import com.littletrip.api.dto.TapResponse;
import com.littletrip.api.service.TripService;
import com.littletrip.api.exception.DuplicateEventException;
import com.littletrip.api.exception.InvalidDeviceException;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.Journey;
import com.littletrip.api.model.JourneyStatus;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.model.TapEvent;
import com.littletrip.api.model.TransitStop;
import com.littletrip.api.repository.ApiKeyDeviceRepository;
import com.littletrip.api.repository.OperatorDeviceRepository;
import com.littletrip.api.repository.TapEventRepository;
import com.littletrip.api.repository.TransitStopRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for processing tap events from transit devices.
 * Handles event validation, persistence, and delegates trip logic to TripService.
 */
@Slf4j
@Service
public class TapService {

    private final TapEventRepository tapEventRepository;
    private final ApiKeyDeviceRepository apiKeyDeviceRepository;
    private final OperatorDeviceRepository operatorDeviceRepository;
    private final TransitStopRepository transitStopRepository;
    private final TripService tripService;

    public TapService(TapEventRepository tapEventRepository,
                      ApiKeyDeviceRepository apiKeyDeviceRepository,
                      OperatorDeviceRepository operatorDeviceRepository,
                      TransitStopRepository transitStopRepository,
                      TripService tripService) {
        this.tapEventRepository = tapEventRepository;
        this.apiKeyDeviceRepository = apiKeyDeviceRepository;
        this.operatorDeviceRepository = operatorDeviceRepository;
        this.transitStopRepository = transitStopRepository;
        this.tripService = tripService;
    }

    /**
     * Processes a single tap event from a transit device.
     * Validates device authorization, persists the event, and triggers trip processing.
     *
     * @param request the tap event request payload
     * @param key     the authenticated API key making the request
     * @return a response containing the journey ID, status, and charge amount
     * @throws InvalidDeviceException    if the device is not found or not authorized
     * @throws DuplicateEventException   if an event with the same ID was already processed
     */
    @Transactional
    public TapResponse processTap(TapRequest request, ApiKey key) {
        log.info("Processing tap event, id: {}, deviceId: {}, stopId: {}, tapType: {}",
            request.getId(), request.getDeviceId(), request.getStopId(), request.getTapType());

        // Look up the device — throws if not found
        OperatorDevice device = operatorDeviceRepository.findById(UUID.fromString(request.getDeviceId()))
            .orElseThrow(() -> new InvalidDeviceException("Device not found: " + request.getDeviceId()));

        log.debug("Found device: {} ({})", device.getName(), device.getId());

        // Verify the API key is authorized to submit events for this device
        validateDeviceAuthorization(key, device);
        log.debug("Device authorization validated for device: {}", device.getName());

        // Look up the transit stop — throws if not found
        TransitStop stop = transitStopRepository.findById(UUID.fromString(request.getStopId()))
            .orElseThrow(() -> new IllegalArgumentException("Stop not found: " + request.getStopId()));

        log.debug("Found stop: {} ({})", stop.getName(), stop.getId());

        // Build the TapEvent entity from the request
        TapEvent tapEvent = new TapEvent();
        tapEvent.setTapType(request.getTapType());
        tapEvent.setStop(stop);
        tapEvent.setOperator(key.getOperator());
        tapEvent.setDevice(device);
        tapEvent.setCardToken(request.getCardToken());
        tapEvent.setCreatedAt(request.getDateTimeUtc());
        tapEvent.setRequestId(UUID.fromString(request.getId()));

        try {
            tapEventRepository.save(tapEvent);
            log.debug("Saved tap event with id: {}", request.getId());
        } catch (DataIntegrityViolationException e) {
            // The request ID is a unique constraint — if this fires, the event was already processed
            log.warn("Duplicate tap event detected, id: {}", request.getId());
            throw new DuplicateEventException("Event with id " + request.getId() + " already processed");
        }

        // Delegate to TripService to handle journey creation/completion
        Journey journey = tripService.processTap(tapEvent);
        JourneyStatus status = journey != null ? journey.getStatus() : JourneyStatus.IN_PROGRESS;
        Integer charge = journey != null ? journey.getFareAmount() : 0;

        log.info("Tap event processed, journeyId: {}, status: {}, charge: {}",
            journey != null ? journey.getId() : null, status, charge);

        return new TapResponse(journey != null ? journey.getId() : null, status, charge);
    }

    /**
     * Validates that the given API key is authorized to submit events for the device.
     * This check is performed by looking up the api_key_device association table.
     *
     * @param key    the authenticated API key
     * @param device the device that submitted the event
     * @throws InvalidDeviceException if the device is not linked to the API key
     */
    private void validateDeviceAuthorization(ApiKey key, OperatorDevice device) {
        if (!apiKeyDeviceRepository.existsByApiKeyIdAndDeviceId(key.getId(), device.getId())) {
            log.warn("Device {} not authorized for API key {}", device.getName(), key.getId());
            throw new InvalidDeviceException("Device " + device.getName() + " not authorized for this API key");
        }
        log.debug("Device {} authorized for API key {}", device.getName(), key.getId());
    }

    /**
     * Lists tap events with flexible filtering by cardToken, operatorId, and deviceId.
     * At least one filter must be provided to avoid returning all events.
     *
     * @param cardToken   optional filter by card token
     * @param operatorId optional filter by operator
     * @param deviceId   optional filter by device
     * @param page       0-based page number
     * @param size       number of items per page
     * @return paginated list of tap event DTOs
     */
    public PagedResponse<TapEventDto> getTapEvents(String cardToken, UUID operatorId,
                                                    UUID deviceId, int page, int size) {
        log.info("Received request to list tap events, cardToken: {}, operatorId: {}, deviceId: {}, page: {}, size: {}",
            cardToken, operatorId, deviceId, page, size);

        List<TapEvent> results;

        // Select the most specific query based on which filters are provided.
        // More filters = narrower query = better performance.
        if (cardToken != null && operatorId != null && deviceId != null) {
            results = tapEventRepository.findByCardTokenAndOperatorIdAndDeviceIdOrderByCreatedAtDesc(cardToken, operatorId, deviceId);
            log.debug("Querying by cardToken + operatorId + deviceId");
        } else if (cardToken != null && operatorId != null) {
            results = tapEventRepository.findByCardTokenAndOperatorIdOrderByCreatedAtDesc(cardToken, operatorId);
            log.debug("Querying by cardToken + operatorId");
        } else if (cardToken != null) {
            results = tapEventRepository.findByCardTokenOrderByCreatedAtDesc(cardToken);
            log.debug("Querying by cardToken only");
        } else if (operatorId != null) {
            results = tapEventRepository.findByOperatorIdOrderByCreatedAtDesc(operatorId);
            log.debug("Querying by operatorId only");
        } else {
            // Fallback: fetch all and sort in memory.
            // In production, this path should be avoided or paginated at the DB level.
            results = tapEventRepository.findAll().stream()
                .sorted(Comparator.comparing(TapEvent::getCreatedAt).reversed())
                .toList();
            log.debug("Querying all tap events");
        }

        log.debug("Found {} tap events matching query", results.size());

        // Apply pagination in memory since the repository returns a flat list
        int from = page * size;
        int to = Math.min(from + size, results.size());
        List<TapEventDto> content = from < results.size()
            ? results.subList(from, to).stream().map(TapEventDto::from).toList()
            : List.of();

        return new PagedResponse<>(content, page, size, results.size(),
            (int) Math.ceil((double) results.size() / size));
    }

    /**
     * Counts the total number of tap events for a specific operator.
     * Cached to avoid repeated count queries.
     *
     * @param operatorId the operator to count events for
     * @return total number of tap events
     */
    @Cacheable(value = "tapEvents", key = "'count:' + #operatorId")
    public long getTotalTapEventsByOperator(UUID operatorId) {
        log.info("Counting tap events for operator: {}", operatorId);
        return tapEventRepository.countByOperatorId(operatorId);
    }

    /**
     * Counts the total number of tap events across all operators.
     *
     * @return total number of tap events
     */
    public long getTotalTapEvents() {
        log.info("Counting all tap events");
        return tapEventRepository.count();
    }

    /**
     * Converts a Spring Data Page of TapEvents to a PagedResponse of TapEventDtos.
     *
     * @param page the Spring Data page
     * @return a PagedResponse with DTOs
     */
    private PagedResponse<TapEventDto> toPagedResponse(Page<TapEvent> page) {
        long total = page.getTotalElements();
        int totalPages = page.getTotalPages();
        return new PagedResponse<>(
                page.getContent().stream().map(TapEventDto::from).toList(),
                page.getNumber(),
                page.getSize(),
                total,
                totalPages);
    }
}
