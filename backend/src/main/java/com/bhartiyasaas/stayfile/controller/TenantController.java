package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;
import com.bhartiyasaas.stayfile.service.TenantService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenant onboarded successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        TenantResponse response = tenantService.getTenantById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getTenantsByOrganization(
            @RequestParam UUID organizationId) {
        List<TenantResponse> response = tenantService.getTenantsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
