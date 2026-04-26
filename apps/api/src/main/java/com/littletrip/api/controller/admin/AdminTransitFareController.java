package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresAdmin;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.TransitFareDto;
import com.littletrip.api.model.TransitFare;
import com.littletrip.api.service.TransitFareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for transit fare management (admin only).
 *
 * Requires basic authentication (admin credentials).
 * Used for managing fare rules between transit stops.
 */
@RateLimit(requests = 100, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresAdmin
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Admin: Transit Fares", description = "Transit fare management for admin.")
@RestController
@RequestMapping("/admin/fares")
public class AdminTransitFareController {

    private final TransitFareService transitFareService;

    /**
     * Constructor injection for TransitFareService dependency.
     * Spring will automatically inject the singleton TransitFareService instance.
     *
     * @param transitFareService The service layer for transit fare business logic
     */
    public AdminTransitFareController(TransitFareService transitFareService) {
        this.transitFareService = transitFareService;
    }

    /**
     * Retrieves all transit fares with optional filtering by stop.
     * Returns fares where either the origin or destination matches the stop ID.
     *
     * @param stopId Optional filter to return fares involving a specific stop
     * @param page Zero-based page number for pagination
     * @param size Number of results per page
     * @return ResponseEntity with paginated transit fare results, HTTP 200 on success
     */
    @GetMapping
    @Operation(summary = "List transit fares", description = "Retrieves transit fares with optional stop filtering")
    public ResponseEntity<PagedResponse<TransitFareDto>> list(
            @RequestParam(required = false) UUID stopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TransitFareDto> response = transitFareService.listFares(stopId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific transit fare by stop pair.
     */
    /**
     * Retrieves a specific transit fare by stop pair (origin and destination).
     * The fare is directional: stopAId is origin, stopBId is destination.
     *
     * @param stopAId The origin stop UUID
     * @param stopBId The destination stop UUID
     * @return ResponseEntity with the transit fare, HTTP 200 if found; HTTP 404 if not found
     */
    @GetMapping("/{stopAId}/{stopBId}")
    @Operation(summary = "Get transit fare", description = "Retrieves a specific transit fare by stop pair",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fare found"),
                    @ApiResponse(responseCode = "404", description = "Fare not found", content = @Content)
            })
    public ResponseEntity<TransitFareDto> get(
            @PathVariable UUID stopAId,
            @PathVariable UUID stopBId) {
        return transitFareService.getFare(stopAId, stopBId)
            .map(f -> ResponseEntity.ok(TransitFareDto.from(f)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing transit fare.
     */
    /**
     * Updates an existing transit fare for a stop pair.
     * Only fare amount and currency can be updated.
     *
     * @param stopAId The origin stop UUID
     * @param stopBId The destination stop UUID
     * @param request Map containing updated fare fields (amount, currency)
     * @return ResponseEntity with the updated transit fare, HTTP 200 if updated; HTTP 404 if fare not found
     */
    @PutMapping("/{stopAId}/{stopBId}")
    @Operation(summary = "Update transit fare", description = "Updates an existing transit fare",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fare updated"),
                    @ApiResponse(responseCode = "404", description = "Fare not found", content = @Content)
            })
    public ResponseEntity<TransitFareDto> update(
            @PathVariable UUID stopAId,
            @PathVariable UUID stopBId,
            @RequestBody Map<String, Object> request) {
        return transitFareService.updateFare(stopAId, stopBId, request)
            .map(f -> ResponseEntity.ok(TransitFareDto.from(f)))
            .orElse(ResponseEntity.notFound().build());
    }
}
