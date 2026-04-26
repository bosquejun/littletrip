package com.littletrip.api.controller;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.annotation.RequiresAdmin;
import com.littletrip.api.dto.PagedResponse;
import com.littletrip.api.dto.OperatorUpdateRequest;
import com.littletrip.api.model.Operator;
import com.littletrip.api.service.OperatorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

/**
 * REST controller for operator management (admin only).
 *
 * Requires basic authentication (admin credentials).
 * Used for managing transit operators in the system.
 */
@RateLimit(requests = 60, per = java.util.concurrent.TimeUnit.MINUTES)
@RequiresAdmin
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Admin: Operators", description = "Operators management for admin.")
@RestController
@RequestMapping("/admin/operators")
public class AdminOperatorController {

    private final OperatorService operatorService;

    /**
     * Constructor injection for OperatorService dependency.
     * Spring will automatically inject the singleton OperatorService instance.
     *
     * @param operatorService The service layer for operator business logic
     */
    public AdminOperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    /**
     * Retrieves all operators with pagination.
     * Returns a paged list of all transit operators in the system.
     *
     * @param page One-based page number for pagination (defaults to 1)
     * @param size Number of results per page (defaults to 20)
     * @return ResponseEntity with paginated operator results, HTTP 200 on success
     */
    @GetMapping
    @Operation(summary = "List operators", description = "Retrieves all operators with pagination")
    public ResponseEntity<PagedResponse<Operator>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<Operator> response = operatorService.listCachedOperators(page, size);
        return ResponseEntity.ok(response);
    }


    /**
     * Get a specific operator by ID.
     */
    /**
     * Retrieves a specific operator by its unique identifier.
     * Returns the full operator details including associated devices and API keys.
     *
     * @param id The UUID of the operator to retrieve
     * @return ResponseEntity with the operator, HTTP 200 if found; HTTP 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get operator", description = "Retrieves a specific operator by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operator found"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<Operator> get(@PathVariable UUID id) {
        return operatorService.getCachedOperatorDetails(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing operator.
     */
    /**
     * Updates an existing operator.
     * Can update the operator name and other attributes.
     *
     * @param id The UUID of the operator to update
     * @param request The update request containing fields to modify
     * @return ResponseEntity with the updated operator, HTTP 200 if updated; HTTP 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update operator", description = "Updates an existing operator",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Operator updated"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<Operator> update(@PathVariable UUID id, @RequestBody OperatorUpdateRequest request) {
        return operatorService.updateOperator(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an operator.
     */
    /**
     * Deletes an operator permanently.
     * This action cannot be undone; the operator and associated data will be removed.
     * Note: This may fail if the operator has associated API keys or devices.
     *
     * @param id The UUID of the operator to delete
     * @return HTTP 204 if deleted successfully; HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete operator", description = "Deletes an operator",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Operator deleted"),
                    @ApiResponse(responseCode = "404", description = "Operator not found", content = @Content)
            })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (operatorService.deleteOperator(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }



}
