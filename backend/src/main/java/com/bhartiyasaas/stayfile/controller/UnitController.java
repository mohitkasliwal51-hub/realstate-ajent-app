package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.UnitCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.UnitResponse;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import com.bhartiyasaas.stayfile.service.UnitService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitCreateRequest request) {
        UnitResponse response = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Unit created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping("/properties/{propertyId}/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getUnitsByProperty(
            @PathVariable UUID propertyId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UnitStatus status) {
        List<UnitResponse> response = unitService.getUnitsByProperty(propertyId, organizationId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping("/units/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnitById(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        UnitResponse response = unitService.getUnitById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PatchMapping("/units/{id}/status")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnitStatus(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @RequestParam UnitStatus status) {
        UnitResponse response = unitService.updateUnitStatus(id, organizationId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit status updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PutMapping("/units/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @Valid @RequestBody UnitCreateRequest request) {
        UnitResponse response = unitService.updateUnit(id, organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @DeleteMapping("/units/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        unitService.deleteUnit(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unit deleted successfully"));
    }
}
