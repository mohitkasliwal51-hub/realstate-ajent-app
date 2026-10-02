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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getMeterReadingsByLease(
            @PathVariable UUID leaseId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MeterReadingResponse> response = meterReadingService.getMeterReadingsByLease(leaseId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
