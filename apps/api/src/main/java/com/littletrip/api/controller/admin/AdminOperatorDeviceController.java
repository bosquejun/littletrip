package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresAdmin;
import com.littletrip.api.dto.OperatorDeviceUpdateRequest;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.service.OperatorDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for operator device management (admin only).
 *
 * Requires basic authentication (admin credentials).
 * Used for managing operator devices (e.g., fare validators) in the system.
 */
@RequiresAdmin
@RateLimit(requests = 60, per = java.util.concurrent.TimeUnit.MINUTES)
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Admin: Operator Devices", description = "Operator devices management for admin.")
@RestController
@RequestMapping("/admin/devices")
public class AdminOperatorDeviceController {

    private final OperatorDeviceService operatorDeviceService;

    /**
     * Constructor injection for OperatorDeviceService dependency.
     * Spring will automatically inject the singleton OperatorDeviceService instance.
     *
     * @param operatorDeviceService The service layer for operator device business logic
     */
    public AdminOperatorDeviceController(OperatorDeviceService operatorDeviceService) {
        this.operatorDeviceService = operatorDeviceService;
    }

    /**
     * Retrieves all operator devices with optional filtering by operator.
     * Returns devices that belong to a specific operator if operatorId is provided.
     *
     * @param operatorId Optional filter to return devices for a specific operator
     * @param page One-based page number for pagination (defaults to 1)
     * @param size Number of results per page (defaults to 20)
     * @return ResponseEntity with paginated operator device results, HTTP 200 on success
     */
    @GetMapping
    @Operation(summary = "List operator devices", description = "Retrieves operator devices with optional operator filtering")
    public ResponseEntity<PagedResponse<OperatorDevice>> list(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<OperatorDevice> response = operatorDeviceService.listCachedOperatorDevices(operatorId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific operator device by ID.
     */
    /**
     * Retrieves a specific operator device by its unique identifier.
     * Returns the full device details including the associated operator.
     *
     * @param id The UUID of the operator device to retrieve
     * @return ResponseEntity with the operator device, HTTP 200 if found; HTTP 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get operator device", description = "Retrieves a specific operator device by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device found"),
                    @ApiResponse(responseCode = "404", description = "Device not found", content = @Content)
            })
    public ResponseEntity<OperatorDevice> getById(@PathVariable UUID id) {
        return operatorDeviceService.getCachedOperatorDeviceDetails(id)
                 .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing operator device.
     */
    /**
     * Updates an existing operator device.
     * Can update the device name and other device attributes.
     *
     * @param id The UUID of the operator device to update
     * @param request The update request containing fields to modify
     * @return ResponseEntity with the updated operator device, HTTP 200 if updated; HTTP 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update operator device", description = "Updates an existing operator device",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device updated"),
                    @ApiResponse(responseCode = "404", description = "Device not found", content = @Content)
            })
    public ResponseEntity<OperatorDevice> update(@PathVariable UUID id, @RequestBody OperatorDeviceUpdateRequest request) {
        return operatorDeviceService.updateOperatorDevice(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
