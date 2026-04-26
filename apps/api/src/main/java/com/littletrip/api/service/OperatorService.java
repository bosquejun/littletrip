package com.littletrip.api.service;

import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.OperatorUpdateRequest;
import com.littletrip.api.model.Operator;
import com.littletrip.api.repository.OperatorRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing transit operators.
 * Provides CRUD operations with caching for reads and cache eviction on writes.
 */
@Slf4j
@Service
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    /**
     * Lists all operators with pagination.
     * Results are cached per page/size combination.
     *
     * @param page 1-based page number
     * @param size number of items per page
     * @return paginated list of operators
     */
    @Cacheable(value = "operators", key = "{#page, #size}")
    public PagedResponse<Operator> listCachedOperators(int page, int size) {
        log.info("Received request to list operators, page: {}, size: {}", page, size);

        Page<Operator> result = operatorRepository.findAll(PageRequest.of(page - 1, size));

        log.debug("Retrieved list of operators, count: {}", result.getNumberOfElements());

        return new PagedResponse<>(
            result.getContent(),
            result.getNumber() + 1,  // convert 0-based to 1-based for API response
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /**
     * Retrieves a single operator by ID.
     * Cached to avoid repeated DB lookups for the same operator.
     *
     * @param id the UUID of the operator
     * @return the operator if found
     */
    @Cacheable(value = "operators", key = "#id.toString()", unless = "#result == null")
    public Optional<Operator> getCachedOperatorDetails(UUID id) {
        log.info("Received request to get operator details, id: {}", id);
        return operatorRepository.findById(id);
    }

    /**
     * Updates an operator's name.
     * Evicts both the operators and apiKeys caches since API keys are associated
     * with operators and may depend on operator data.
     *
     * @param id      the UUID of the operator to update
     * @param request the update request containing an optional name field
     * @return the updated operator if found
     */
    @Caching(evict = {
        @CacheEvict(value = "operators", allEntries = true),
        @CacheEvict(value = "apiKeys", allEntries = true)
    })
    public Optional<Operator> updateOperator(UUID id, OperatorUpdateRequest request) {
        log.info("Received request to update operator, id: {}, input: {}", id, request);
        return operatorRepository.findById(id).map(existing -> {
            log.debug("Found existing operator, proceeding with updates");
            if (request.name() != null) {
                existing.setName(request.name());
            }

            Operator saved = operatorRepository.save(existing);
            log.debug("Updated operator, id: {}", id);

            return saved;
        });
    }

    /**
     * Deletes an operator by ID.
     * Evicts both operators and apiKeys caches to maintain consistency,
     * since API keys reference operators.
     *
     * @param id the UUID of the operator to delete
     * @return true if the operator was found and deleted, false otherwise
     */
    @Caching(evict = {
        @CacheEvict(value = "operators", allEntries = true),
        @CacheEvict(value = "apiKeys", allEntries = true)
    })
    public boolean deleteOperator(UUID id) {
        log.info("Received request to delete operator, id: {}", id);
        if (operatorRepository.existsById(id)) {
            operatorRepository.deleteById(id);
            log.debug("Deleted operator, id: {}", id);
            return true;
        }
        log.debug("Operator not found for deletion, id: {}", id);
        return false;
    }

}