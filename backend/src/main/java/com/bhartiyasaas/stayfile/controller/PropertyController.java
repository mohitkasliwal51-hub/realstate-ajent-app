package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(@Valid @RequestBody PropertyCreateRequest request) {
        PropertyResponse response = propertyService.createProperty(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Property created successfully"));
    }

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        PropertyResponse response = propertyService.getPropertyById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPropertiesByOrganization(
            @RequestParam UUID organizationId) {
        List<PropertyResponse> response = propertyService.getPropertiesByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPublicShowcaseProperties() {
        List<PropertyResponse> response = propertyService.getPublicShowcaseProperties();
        return ResponseEntity.ok(ApiResponse.success(response, "Public properties retrieved successfully"));
    }
}
