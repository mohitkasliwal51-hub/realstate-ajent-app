package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.dto.request.PanVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.PanVerificationResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.KycVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bhartiyasaas.stayfile.dto.response.AadhaarMaskingResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.DigiLockerInitiateRequest;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerVerificationCompleteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycVerificationService kycVerificationService;

    @PostMapping("/pan/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<PanVerificationResponse>> verifyPan(
            @Valid @RequestBody PanVerificationRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        PanVerificationResponse response = kycVerificationService.verifyPan(request, currentUser);
        String message = response.isValid() ? "PAN verified successfully" : "PAN verification returned invalid";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping(value = "/aadhaar/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<AadhaarMaskingResponse>> maskAadhaar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tenantId", required = false) UUID tenantId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        AadhaarMaskingResponse response = kycVerificationService.maskAadhaar(file, tenantId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/digilocker/initiate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<DigiLockerInitiateResponse>> initiateDigiLocker(
            @Valid @RequestBody(required = false) DigiLockerInitiateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        if (request == null) {
            request = new DigiLockerInitiateRequest();
        }
        DigiLockerInitiateResponse response = kycVerificationService.initiateDigiLocker(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/digilocker/status/{verificationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<com.bhartiyasaas.stayfile.dto.response.DigiLockerStatusResponse>> getDigiLockerStatus(
            @PathVariable String verificationId,
            @RequestParam(value = "tenantId", required = false) UUID tenantId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        com.bhartiyasaas.stayfile.dto.response.DigiLockerStatusResponse response = kycVerificationService.getDigiLockerStatus(verificationId, tenantId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/digilocker/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<DigiLockerVerificationCompleteResponse>> completeDigiLocker(
            @RequestParam("verificationId") String verificationId,
            @RequestParam(value = "tenantId", required = false) UUID tenantId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        DigiLockerVerificationCompleteResponse response = kycVerificationService.completeDigiLocker(verificationId, tenantId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}
