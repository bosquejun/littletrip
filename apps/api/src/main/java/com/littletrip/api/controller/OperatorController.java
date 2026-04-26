package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresAdmin;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.OperatorUpdateRequest;
import com.littletrip.api.model.Operator;
import com.littletrip.api.model.OperatorDevice;
import com.littletrip.api.service.OperatorService;
import com.littletrip.api.service.OperatorDeviceService;

import com.littletrip.api.model.ApiKey;
import com.littletrip.api.annotation.RequiresApiKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RateLimit(requests = 100, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresApiKey
@SecurityRequirement(name = "apiKey")
@Tag(name = "Operator", description = "Operator endpoints")
@RestController
@RequestMapping("/operator")
public class OperatorController {

    private static final Logger log = LoggerFactory.getLogger(OperatorController.class);

    private final OperatorService operatorService;
    private final OperatorDeviceService operatorDeviceService;

    public OperatorController(OperatorService operatorService, OperatorDeviceService operatorDeviceService) {
        this.operatorService = operatorService;
        this.operatorDeviceService = operatorDeviceService;
    }

    @GetMapping
    @Operation(summary = "Get operator", description = "Retrieves a specific operator by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operator found"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<Operator> get(Authentication auth) {
        log.debug("Get operator request received");

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            log.warn("Get operator failed: invalid or missing API key");
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }

        // Get operator ID to scope the query to the authenticated operator
        UUID operatorId = apiKey.getOperator().getId();
        log.debug("Fetching operator details for operatorId={}", operatorId);

        return operatorService.getCachedOperatorDetails(operatorId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

     @GetMapping("/devices")
    @Operation(summary = "Get operator devices", description = "Retrieves a specific operator by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operator found"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<PagedResponse<OperatorDevice>> getDevices(Authentication auth, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Get devices request received - page={}, size={}", page, size);

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            log.warn("Get devices failed: invalid or missing API key");
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }

        // Get operator ID to scope the query to the authenticated operator
        UUID operatorId = apiKey.getOperator().getId();
        log.debug("Fetching devices for operatorId={}", operatorId);

        PagedResponse<OperatorDevice> response = operatorDeviceService.listCachedOperatorDevices(operatorId, page, size);
        return ResponseEntity.ok(response);
    }


    @PutMapping()
    @Operation(summary = "Update operator", description = "Updates an existing operator",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operator updated"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<Operator> update(Authentication auth, @RequestBody OperatorUpdateRequest request) {
        log.debug("Update operator request received");

        // Validates that authentication is present and extracts the ApiKey principal
        if (auth == null || !(auth.getPrincipal() instanceof ApiKey apiKey)) {
            log.warn("Update operator failed: invalid or missing API key");
            throw new org.springframework.security.access.AccessDeniedException("Invalid or missing API key");
        }

        // Get operator ID to scope the query to the authenticated operator
        UUID operatorId = apiKey.getOperator().getId();
        log.info("Updating operator with operatorId={}", operatorId);

        return operatorService.updateOperator(operatorId, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }


}
