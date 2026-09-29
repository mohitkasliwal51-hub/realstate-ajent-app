package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.service.LeaseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<LeaseResponse>> createLease(@Valid @RequestBody LeaseCreateRequest request) {
        LeaseResponse response = leaseService.createLease(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Rent agreement created successfully"));
    }

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT', 'TENANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaseResponse>> getLeaseById(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        LeaseResponse response = leaseService.getLeaseById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaseResponse>>> getLeasesByOrganization(
            @RequestParam UUID organizationId) {
        List<LeaseResponse> response = leaseService.getLeasesByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT', 'TENANT')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadLeasePdf(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        byte[] pdfBytes = leaseService.getLeasePdf(id, organizationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "rent_agreement_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
