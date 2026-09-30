package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<PropertyLeadResponse>> createLead(@Valid @RequestBody PropertyLeadCreateRequest request) {
        PropertyLeadResponse response = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Inquiry captured successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyLeadResponse>>> getLeadsByOrganization(@RequestParam UUID organizationId) {
        List<PropertyLeadResponse> response = leadService.getLeadsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PropertyLeadResponse>> updateLeadStatus(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @RequestParam LeadStatus status) {
        PropertyLeadResponse response = leadService.updateLeadStatus(id, organizationId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Lead status updated to " + status));
    }
}
