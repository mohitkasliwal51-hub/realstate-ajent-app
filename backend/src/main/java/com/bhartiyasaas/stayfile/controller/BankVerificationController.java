package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.dto.request.BankIfscLookupRequest;
import com.bhartiyasaas.stayfile.dto.request.BankRpdInitiateRequest;
import com.bhartiyasaas.stayfile.dto.request.BankSyncVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.BankUpiVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.BankIfscLookupResponse;
import com.bhartiyasaas.stayfile.dto.response.BankRpdInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.BankVerificationResultResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.BankVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank-verification")
@RequiredArgsConstructor
public class BankVerificationController {

    private final BankVerificationService bankVerificationService;

    @PostMapping("/rpd/initiate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<BankRpdInitiateResponse>> initiateReversePennyDrop(
            @Valid @RequestBody BankRpdInitiateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        BankRpdInitiateResponse response = bankVerificationService.initiateReversePennyDrop(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/rpd/status/{verificationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<BankVerificationResultResponse>> checkReversePennyDropStatus(
            @PathVariable String verificationId,
            @RequestParam(required = false) UUID landlordId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        BankVerificationResultResponse response = bankVerificationService.checkReversePennyDropStatus(verificationId, landlordId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/ifsc/lookup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<BankIfscLookupResponse>> lookupIfsc(
            @Valid @RequestBody BankIfscLookupRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        BankIfscLookupResponse response = bankVerificationService.lookupIfsc(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/sync/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<BankVerificationResultResponse>> verifyBankAccountSync(
            @Valid @RequestBody BankSyncVerificationRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        BankVerificationResultResponse response = bankVerificationService.verifyBankAccountSync(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/upi/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    public ResponseEntity<ApiResponse<BankVerificationResultResponse>> verifyUpiPennyDrop(
            @Valid @RequestBody BankUpiVerificationRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        BankVerificationResultResponse response = bankVerificationService.verifyUpiPennyDrop(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}
