package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.MeterReadingRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.MeterReadingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;
    private final com.bhartiyasaas.stayfile.service.UtilitySplitService utilitySplitService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @PostMapping
    public ResponseEntity<ApiResponse<MeterReadingResponse>> createMeterReading(
            @Valid @RequestBody MeterReadingRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        MeterReadingResponse response = meterReadingService.createMeterReading(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Meter reading recorded successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getMeterReadingById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        MeterReadingResponse response = meterReadingService.getMeterReadingById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getMeterReadingsByUnit(
            @PathVariable UUID unitId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MeterReadingResponse> response = meterReadingService.getMeterReadingsByUnit(unitId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getMeterReadingsByProperty(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MeterReadingResponse> response = meterReadingService.getMeterReadingsByProperty(propertyId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getMeterReadingsByLease(
            @PathVariable UUID leaseId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MeterReadingResponse> response = meterReadingService.getMeterReadingsByLease(leaseId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getMeterReadingsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MeterReadingResponse> response = meterReadingService.getMeterReadingsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> verifyMeterReading(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String disputeReason,
            @AuthenticationPrincipal SecurityUser currentUser) {
        MeterReadingResponse response = meterReadingService.verifyMeterReading(id, status, disputeReason, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Meter reading verification status updated"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/split/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<com.bhartiyasaas.stayfile.dto.response.InvoiceResponse>>> runUtilitySplit(
            @PathVariable UUID propertyId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate billingMonth,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<com.bhartiyasaas.stayfile.dto.response.InvoiceResponse> responses =
                utilitySplitService.calculateAndGeneratePgUtilityInvoices(propertyId, billingMonth, currentUser);
        return ResponseEntity.ok(ApiResponse.success(responses, "Utility invoices generated successfully via PG split engine"));
    }
}
