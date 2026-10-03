package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.LandlordRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.LandlordResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.LandlordService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/landlords")
@RequiredArgsConstructor
public class LandlordController {

    private final LandlordService landlordService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<LandlordResponse>> createLandlord(
            @Valid @RequestBody LandlordRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordResponse response = landlordService.createLandlord(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Landlord created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LandlordResponse>> getLandlordById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordResponse response = landlordService.getLandlordById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LandlordResponse>>> getLandlordsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<LandlordResponse> response = landlordService.getLandlordsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LandlordResponse>> updateLandlord(
            @PathVariable UUID id,
            @Valid @RequestBody LandlordRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordResponse response = landlordService.updateLandlord(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Landlord updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLandlord(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        landlordService.deleteLandlord(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Landlord deactivated successfully"));
    }
}
