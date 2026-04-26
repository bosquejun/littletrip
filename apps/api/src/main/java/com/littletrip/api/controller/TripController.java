package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresApiKey;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TripDto;
import com.littletrip.api.model.ApiKey;
import com.littletrip.api.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for trip querying.
 *
 * Requires X-API-Key authentication for all endpoints.
 * All queries are scoped to the operator associated with the API key.
 */
@RateLimit(requests = 100, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresApiKey
@SecurityRequirement(name = "apiKey")
@Tag(name = "Trips", description = "Trip querying and management")
@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    /**
     * Constructor injection for TripService dependency.
     * Spring will automatically inject the singleton TripService instance.
     *
     * @param tripService The service layer for trip business logic
     */
    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * Retrieves paginated trips for the operator associated with the API key.
     * Results can be filtered by cardToken.
     *
     * @param auth The authentication object containing the API key principal
     * @param cardToken Optional filter for specific card token
     * @param page Zero-based page number for pagination
     * @param size Number of results per page
     * @return ResponseEntity with paginated trip results, HTTP 200 on success
     * @throws AccessDeniedException if the API key is invalid or missing
     */
    @GetMapping
    @Operation(summary = "Query trips", description = "Retrieves paginated trips for the operator",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved trips"),
                    @ApiResponse(responseCode = "403", description = "Invalid or missing API key", content = @Content)
            })
    public ResponseEntity<PagedResponse<TripDto>> getAllTrips(
            Authentication auth,
            @RequestParam(required = false) String cardToken,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }
        // Get operator ID to scope the query to the authenticated operator
        UUID operatorId = apiKey.getOperator().getId();

        // Retrieve paginated trips with optional card token filter
        PagedResponse<TripDto> response = tripService.getTrips(
                cardToken, operatorId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific trip by its unique identifier.
     * The trip must belong to the operator associated with the API key.
     *
     * @param auth The authentication object containing the API key principal
     * @param id The UUID of the trip to retrieve
     * @return ResponseEntity with the trip, HTTP 200 if found; HTTP 404 if not found
     * @throws AccessDeniedException if the API key is invalid or missing
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get trip by ID", description = "Retrieves a specific trip by its ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved trip"),
                    @ApiResponse(responseCode = "404", description = "Trip not found", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Invalid or missing API key", content = @Content)
            })
    public ResponseEntity<TripDto> getTripById(
            Authentication auth,
            @PathVariable UUID id) {

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }
        // Retrieve the trip by ID
        TripDto trip = tripService.getTripById(id);
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trip);
    }
}
