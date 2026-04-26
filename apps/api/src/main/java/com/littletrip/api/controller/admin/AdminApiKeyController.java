package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresAdmin;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.ApiKeyUpdateRequest;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.service.ApiKeyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

/**
 * REST controller for API key management (admin only).
 *
 * Requires basic authentication (admin credentials).
 * Used for managing API keys that authorize transit operators to ingest tap events.
 */
@RateLimit(requests = 60, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresAdmin
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Admin: API Keys", description = "API Keys management for admin.")
@RestController
@RequestMapping("/admin/api-keys")
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Constructor injection for ApiKeyService dependency.
     * Spring will automatically inject the singleton ApiKeyService instance.
     *
     * @param apiKeyService The service layer for API key business logic
     */
    public AdminApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * Retrieves all API keys with pagination.
     * Returns a paged list of all API keys in the system.
     *
     * @param page One-based page number for pagination (defaults to 1)
     * @param size Number of results per page (defaults to 20)
     * @return ResponseEntity with paginated API key results, HTTP 200 on success
     */
    @GetMapping
    @Operation(summary = "List API keys", description = "Retrieves all API keys with pagination")
    public ResponseEntity<PagedResponse<ApiKey>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ApiKey> response = apiKeyService.listCachedApiKeys(page, size);
        return ResponseEntity.ok(response);
    }


    /**
     * Get a specific API key by ID.
     */
    /**
     * Retrieves a specific API key by its unique identifier.
     * Returns the full API key details including the associated operator.
     *
     * @param id The UUID of the API key to retrieve
     * @return ResponseEntity with the API key, HTTP 200 if found; HTTP 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get API key", description = "Retrieves a specific API key by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "API key found"),
                    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
            })
    public ResponseEntity<ApiKey> get(@PathVariable UUID id) {
        return apiKeyService.getCachedApiKeyDetails(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing API key.
     */
    /**
     * Updates an existing API key.
     * Can update the key name, enabled status, and expiration date.
     *
     * @param id The UUID of the API key to update
     * @param request The update request containing fields to modify
     * @return ResponseEntity with the updated API key, HTTP 200 if updated; HTTP 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update API key", description = "Updates an existing API key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "API key updated"),
                    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
            })
    public ResponseEntity<ApiKey> update(@PathVariable UUID id, @RequestBody ApiKeyUpdateRequest request) {
        return apiKeyService.updateApiKey(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an API key.
     */
    /**
     * Deletes an API key permanently.
     * This action cannot be undone; the key will no longer be valid for authentication.
     *
     * @param id The UUID of the API key to delete
     * @return HTTP 204 if deleted successfully; HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete API key", description = "Deletes an API key",
            responses = {
                    @ApiResponse(responseCode = "204", description = "API key deleted"),
                    @ApiResponse(responseCode = "404", description = "API key not found", content = @Content)
            })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (apiKeyService.deleteApiKey(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }



}
