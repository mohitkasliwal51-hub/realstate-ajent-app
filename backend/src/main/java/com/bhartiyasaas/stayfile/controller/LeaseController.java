package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.service.LeaseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<LeaseResponse>> createLease(
            @Valid @RequestBody LeaseCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LeaseResponse response = leaseService.createLease(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Rent agreement created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaseResponse>> getLeaseById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LeaseResponse response = leaseService.getLeaseById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaseResponse>>> getLeasesByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<LeaseResponse> response = leaseService.getLeasesByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LeaseResponse>> updateLeaseStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam LeaseStatus status) {
        LeaseResponse response = leaseService.updateLeaseStatus(id, currentUser, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Lease status updated to " + status));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadLeasePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        byte[] pdfBytes = leaseService.getLeasePdf(id, currentUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "rent_agreement_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
