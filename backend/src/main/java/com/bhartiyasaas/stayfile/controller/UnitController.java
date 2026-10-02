package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bhartiyasaas.stayfile.security.SecurityUser;

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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(
            @Valid @RequestBody UnitCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        UnitResponse response = unitService.createUnit(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Unit created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/properties/{propertyId}/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getUnitsByProperty(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(required = false) UnitStatus status) {
        List<UnitResponse> response = unitService.getUnitsByProperty(propertyId, currentUser, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/units/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnitById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        UnitResponse response = unitService.getUnitById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PatchMapping("/units/{id}/status")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnitStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam UnitStatus status) {
        UnitResponse response = unitService.updateUnitStatus(id, currentUser, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit status updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PutMapping("/units/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody UnitCreateRequest request) {
        UnitResponse response = unitService.updateUnit(id, currentUser, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @DeleteMapping("/units/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        unitService.deleteUnit(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Unit deleted successfully"));
    }
}
