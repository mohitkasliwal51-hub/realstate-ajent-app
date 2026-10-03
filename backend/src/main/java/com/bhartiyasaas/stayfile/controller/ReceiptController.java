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

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.service.ReceiptService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptResponse>> createReceipt(
            @Valid @RequestBody ReceiptCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        ReceiptResponse response = receiptService.createReceipt(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Receipt generated successfully"));
    }

    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getCurrentUserReceipts(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<ReceiptResponse> response = receiptService.getReceiptsForCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        ReceiptResponse response = receiptService.getReceiptById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getReceiptsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<ReceiptResponse> response = receiptService.getReceiptsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        byte[] pdfBytes = receiptService.getReceiptPdf(id, currentUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "receipt_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
