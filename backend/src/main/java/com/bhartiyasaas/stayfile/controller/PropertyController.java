package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.service.PropertyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @Valid @RequestBody PropertyCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        PropertyResponse response = propertyService.createProperty(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Property created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        PropertyResponse response = propertyService.getPropertyById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPropertiesByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<PropertyResponse> response = propertyService.getPropertiesByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPublicShowcaseProperties() {
        List<PropertyResponse> response = propertyService.getPublicShowcaseProperties();
        return ResponseEntity.ok(ApiResponse.success(response, "Public properties retrieved successfully"));
    }
}
