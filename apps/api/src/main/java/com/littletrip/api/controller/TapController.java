package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresApiKey;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TapEventDto;
import com.littletrip.api.dto.TapRequest;
import com.littletrip.api.dto.TapResponse;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.service.TapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tap event ingestion and querying.
 *
 * Requires X-API-Key authentication for all endpoints.
 * All queries are scoped to the operator associated with the API key.
 */
@RateLimit(requests = 100, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresApiKey
@SecurityRequirement(name = "apiKey")
@Tag(name = "Taps", description = "Tap event ingestion and querying")
@RestController
@RequestMapping("/taps")
public class TapController {

    private final TapService tapService;

    /**
     * Constructor injection for TapService dependency.
     * Spring will automatically inject the singleton TapService instance.
     *
     * @param tapService The service layer for tap event business logic
     */
    public TapController(TapService tapService) {
        this.tapService = tapService;
    }

    /**
     * Ingests a single tap event from a transit device (tap ON or tap OFF).
     * The event is associated with the operator identified by the API key.
     *
     * @param request The tap event data to ingest (contains deviceId, cardToken, tapType, timestamp)
     * @param auth The authentication object containing the API key principal
     * @return ResponseEntity with the created tap event response (including generated ID), HTTP 201 on success
     * @throws AccessDeniedException if the API key is invalid or missing
     */
    @RateLimit(requests = 20, per = java.util.concurrent.TimeUnit.SECONDS, keyParams = {"deviceId", "cardToken"})
    @PostMapping
    @Operation(summary = "Ingest tap event", description = "Records a tap ON/OFF event from a transit device",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Tap event recorded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Invalid or missing API key", content = @Content)
            })
    public ResponseEntity<TapResponse> ingestTap(
            @Valid @RequestBody TapRequest request,
            Authentication auth) {
        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }
        // Process the tap event and associate it with the operator from the API key
        TapResponse response = tapService.processTap(request, apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves paginated tap events for the operator associated with the API key.
     * Results can be filtered by cardToken and/or deviceId.
     *
     * @param auth The authentication object containing the API key principal
     * @param cardToken Optional filter for specific card token
     * @param deviceId Optional filter for specific device ID
     * @param page Zero-based page number for pagination
     * @param size Number of results per page
     * @return ResponseEntity with paginated tap event results, HTTP 200 on success
     * @throws AccessDeniedException if the API key is invalid or missing
     */
    @GetMapping
    @Operation(summary = "Query tap events", description = "Retrieves paginated tap events for the operator",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved tap events"),
                    @ApiResponse(responseCode = "403", description = "Invalid or missing API key", content = @Content)
            })
    public ResponseEntity<PagedResponse<TapEventDto>> getTapEvents(
            Authentication auth,
            @RequestParam(required = false) String cardToken,
            @RequestParam(required = false) UUID deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }
        // Get operator ID to scope the query to the authenticated operator
        UUID operatorId = apiKey.getOperator().getId();

        // Retrieve paginated tap events with optional filters
        PagedResponse<TapEventDto> response = tapService.getTapEvents(
                cardToken, operatorId, deviceId, page, size);
        return ResponseEntity.ok(response);
    }
}
