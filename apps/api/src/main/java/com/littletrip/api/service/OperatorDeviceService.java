package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.OperatorDeviceUpdateRequest;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.model.Operator;
import com.littletrip.api.repository.OperatorDeviceRepository;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing operator devices.
 * Provides read and update operations with caching support.
 */
@Slf4j
@Service
public class OperatorDeviceService {

    private final OperatorDeviceRepository operatorDeviceRepository;

    public OperatorDeviceService(OperatorDeviceRepository operatorDeviceRepository) {
        this.operatorDeviceRepository = operatorDeviceRepository;
    }

    /**
     * Lists operator devices with optional filtering by operator ID.
     * Results are cached to speed up repeated queries.
     *
     * @param operatorId optional filter — if provided, only returns devices for that operator
     * @param page       1-based page number
     * @param size       number of items per page
     * @return paginated list of operator devices
     */
    @Cacheable(value = "operatorDevices", key = "{#operatorId, #page, #size}")
    public PagedResponse<OperatorDevice> listCachedOperatorDevices(UUID operatorId, int page, int size) {
        log.info("Received request to list operator devices, operatorId: {}, page: {}, size: {}", operatorId, page, size);

        Page<OperatorDevice> result;
        if (operatorId != null) {
            // Filter by operator — use 0-based pagination (Spring Data convention)
            log.debug("Filtering operator devices by operatorId: {}", operatorId);
            result = operatorDeviceRepository.findByOperatorId(operatorId, PageRequest.of(page - 1, size));
        } else {
            // No filter — return all devices
            log.debug("Listing all operator devices (no operator filter)");
            result = operatorDeviceRepository.findAll(PageRequest.of(page - 1, size));
        }

        log.debug("Retrieved list of operator devices, count: {}", result.getNumberOfElements());

        // Unproxy lazy-loaded Operator entities to avoid HibernateProxy serialization issues
        result.getContent().forEach(operatorDevice -> {
            if (operatorDevice.getOperator() instanceof HibernateProxy) {
                operatorDevice.setOperator((Operator) ((HibernateProxy) operatorDevice.getOperator())
                    .getHibernateLazyInitializer().getImplementation());
            }
        });

        return new PagedResponse<>(
            result.getContent(),
            result.getNumber() + 1,  // convert 0-based to 1-based page number
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /**
     * Retrieves a single operator device by ID.
     * Cached based on the device UUID.
     *
     * @param id the UUID of the device
     * @return the device if found
     */
    @Cacheable(value = "operatorDevices", key = "#id.toString()", unless = "#result == null")
    public Optional<OperatorDevice> getCachedOperatorDeviceDetails(UUID id) {
        log.info("Received request to get operator device details, id: {}", id);
        return operatorDeviceRepository.findById(id);
    }

    /**
     * Updates an operator device's name, status, or last-seen timestamp.
     * Evicts the entire operatorDevices cache to maintain consistency.
     *
     * @param id      the UUID of the device to update
     * @param request the update request with optional name, status, and lastSeenAt fields
     * @return the updated device if found
     */
    @CacheEvict(value = "operatorDevices", allEntries = true)
    public Optional<OperatorDevice> updateOperatorDevice(UUID id, OperatorDeviceUpdateRequest request) {
        log.info("Received request to update operator device, id: {}, input: {}", id, request);

        return operatorDeviceRepository.findById(id).map(existing -> {
            log.debug("Found existing operator device, proceeding with updates");
            // Apply only the fields provided in the request
            if (request.name() != null) {
                existing.setName(request.name());
            }
            if (request.status() != null) {
                // Status is stored as a string, so we convert the enum to its name
                existing.setStatus(request.status().name());
            }
            if (request.lastSeenAt() != null) {
                // Convert from request DTO time type to Instant for storage
                existing.setLastSeenAt(request.lastSeenAt().toInstant());
            }

            OperatorDevice saved = operatorDeviceRepository.save(existing);
            log.debug("Updated operator device, id: {}", id);
            return saved;
        });
    }

}