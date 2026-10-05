package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.bhartiyasaas.stayfile.dto.request.LandlordPayoutRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.LandlordPayoutResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.LandlordPayoutService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/landlord-payouts")
@RequiredArgsConstructor
public class LandlordPayoutController {

    private final LandlordPayoutService landlordPayoutService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<LandlordPayoutResponse>> createPayout(
            @Valid @RequestBody LandlordPayoutRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordPayoutResponse response = landlordPayoutService.createPayout(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Landlord payout created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LandlordPayoutResponse>> getPayoutById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordPayoutResponse response = landlordPayoutService.getPayoutById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/landlord/{landlordId}")
    public ResponseEntity<ApiResponse<List<LandlordPayoutResponse>>> getPayoutsByLandlord(
            @PathVariable UUID landlordId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<LandlordPayoutResponse> response = landlordPayoutService.getPayoutsByLandlord(landlordId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LandlordPayoutResponse>>> getPayoutsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<LandlordPayoutResponse> response = landlordPayoutService.getPayoutsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/calculate/{landlordId}")
    public ResponseEntity<ApiResponse<LandlordPayoutResponse>> calculateMonthlyPayout(
            @PathVariable UUID landlordId,
            @RequestParam(required = false) String periodMonth,
            @AuthenticationPrincipal SecurityUser currentUser) {
        LandlordPayoutResponse response = landlordPayoutService.calculateMonthlyPayoutForLandlord(landlordId, periodMonth, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Monthly landlord payout calculated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LandlordPayoutResponse>> updatePayoutStatus(
            @PathVariable UUID id,
            @RequestBody(required = false) com.bhartiyasaas.stayfile.dto.request.PayoutStatusUpdateRequest body,
            @RequestParam(required = false) com.bhartiyasaas.stayfile.entity.enums.PayoutStatus status,
            @RequestParam(required = false) String utrNumber,
            @RequestParam(required = false) java.time.LocalDate payoutDate,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal SecurityUser currentUser) {
        com.bhartiyasaas.stayfile.dto.request.PayoutStatusUpdateRequest request = body != null ? body : new com.bhartiyasaas.stayfile.dto.request.PayoutStatusUpdateRequest();
        if (status != null) request.setPayoutStatus(status);
        if (utrNumber != null) request.setUtrNumber(utrNumber);
        if (payoutDate != null) request.setPayoutDate(payoutDate);
        if (notes != null) request.setNotes(notes);
        if (request.getPayoutStatus() == null) {
            throw new com.bhartiyasaas.stayfile.exception.BadRequestException("Payout status is required");
        }
        LandlordPayoutResponse response = landlordPayoutService.updatePayoutStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Payout status updated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPayoutPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        byte[] pdfBytes = landlordPayoutService.downloadPayoutPdf(id, currentUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "payout-statement-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
