package com.littletrip.api.service;

import com.littletrip.api.dto.ApiKeyUpdateRequest;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.model.Operator;
import com.littletrip.api.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing API keys.
 * Provides CRUD operations with caching for read operations.
 */
@Slf4j
@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Lists all API keys with pagination.
     * Results are cached to improve performance for frequently accessed pages.
     *
     * @param page 1-based page number
     * @param size number of items per page
     * @return paginated list of API keys
     */
    @Cacheable(value = "apiKeys", key = "{#page, #size}")
    public PagedResponse<ApiKey> listCachedApiKeys(int page, int size) {
        log.info("Received request to list api keys, page: {}, size: {}", page, size);

        // Spring Data uses 0-based pagination, so we subtract 1 from page
        Page<ApiKey> result = apiKeyRepository.findAll(PageRequest.of(page - 1, size));

        log.debug("Retrieved list of api keys, length: {}", result.getNumberOfElements());

        // Hibernate may return a lazy-loaded proxy for the Operator entity.
        // Detach the proxy so the operator data is fully loaded before serialization.
        result.getContent().forEach(apiKey -> {
            if (apiKey.getOperator() instanceof HibernateProxy) {
                apiKey.setOperator((Operator) ((HibernateProxy) apiKey.getOperator())
                    .getHibernateLazyInitializer().getImplementation());
            }
        });

        return new PagedResponse<>(
            result.getContent(),
            result.getNumber() + 1,  // convert back to 1-based for API response
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /**
     * Retrieves details for a single API key by ID.
     * Cached to avoid repeated database hits for the same key.
     *
     * @param id the UUID of the API key
     * @return the API key if found
     */
    @Cacheable(value = "apiKeys", key = "#id.toString()", unless = "#result == null")
    public Optional<ApiKey> getCachedApiKeyDetails(UUID id) {
        log.info("Received request to get api key details, id: {}", id);
        return apiKeyRepository.findById(id).map(apiKey -> {
            // Unproxy the Operator entity to avoid serialization issues with HibernateProxy
            if (apiKey.getOperator() instanceof HibernateProxy proxy) {
                apiKey.setOperator((Operator) proxy.getHibernateLazyInitializer().getImplementation());
            }
            return apiKey;
        });
    }

    /**
     * Updates an existing API key's name and/or active status.
     * Evicts the entire apiKeys cache to ensure consistency.
     *
     * @param id      the UUID of the API key to update
     * @param request the update request containing optional name and active fields
     * @return the updated API key if found
     */
    @CacheEvict(value = "apiKeys", allEntries = true)
    public Optional<ApiKey> updateApiKey(UUID id, ApiKeyUpdateRequest request) {
        log.info("Received request to update api key, id: {}, input: {}", id, request);
        return apiKeyRepository.findById(id).map(existing -> {
            // Only update fields that are provided in the request
            if (request.name() != null) {
                existing.setName(request.name());
            }
            if (request.active() != null) {
                existing.setActive(request.active());
            }

            ApiKey saved = apiKeyRepository.save(existing);

            log.debug("Updated api key, id: {}", id);

            // Unproxy operator before returning to avoid serialization issues
            if (saved.getOperator() instanceof HibernateProxy proxy) {
                saved.setOperator((Operator) proxy.getHibernateLazyInitializer().getImplementation());
            }

            return saved;
        });
    }

    /**
     * Deletes an API key by ID.
     * Evicts the entire apiKeys cache after deletion.
     *
     * @param id the UUID of the API key to delete
     * @return true if the key was found and deleted, false otherwise
     */
    @CacheEvict(value = "apiKeys", allEntries = true)
    public boolean deleteApiKey(UUID id) {
        log.info("Received request to delete api key, id: {}", id);
        if (apiKeyRepository.existsById(id)) {
            apiKeyRepository.deleteById(id);
            log.debug("Deleted api key, id: {}", id);
            return true;
        }
        log.debug("Api key not found for deletion, id: {}", id);
        return false;
    }

}