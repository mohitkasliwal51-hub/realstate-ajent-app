package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.dto.request.*;
import com.bhartiyasaas.stayfile.dto.response.*;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.KybVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyb")
@RequiredArgsConstructor
public class KybController {
    private final KybVerificationService kybVerificationService;

    @PostMapping("/gstin/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<GstinVerificationResponse>> verifyGstin(
            @Valid @RequestBody GstinVerificationRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        GstinVerificationResponse response = kybVerificationService.verifyGstin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/pan-to-gstin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<PanToGstinResponse>> findGstinsByPan(
            @Valid @RequestBody PanToGstinRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                kybVerificationService.findGstinsByPan(request, currentUser), "PAN-to-GSTIN lookup completed"));
    }

    @PostMapping("/cin/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<CinVerificationResponse>> verifyCin(
            @Valid @RequestBody CinVerificationRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                kybVerificationService.verifyCin(request, currentUser), "CIN verification completed"));
    }
}
