package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.request.PropertyLeadCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.PropertyLeadResponse;
import com.bhartiyasaas.stayfile.entity.enums.LeadStatus;
import com.bhartiyasaas.stayfile.service.PropertyLeadService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class PropertyLeadController {

    private final PropertyLeadService leadService;

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyLeadResponse>> createLead(
            @Valid @RequestBody PropertyLeadCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        PropertyLeadResponse response = leadService.createLead(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Inquiry captured successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyLeadResponse>>> getLeadsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<PropertyLeadResponse> response = leadService.getLeadsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PropertyLeadResponse>> updateLeadStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam LeadStatus status) {
        PropertyLeadResponse response = leadService.updateLeadStatus(id, currentUser, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Lead status updated to " + status));
    }
}
