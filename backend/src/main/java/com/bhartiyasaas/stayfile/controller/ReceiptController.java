package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptResponse>> createReceipt(@Valid @RequestBody ReceiptCreateRequest request) {
        ReceiptResponse response = receiptService.createReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Receipt generated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptById(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        ReceiptResponse response = receiptService.getReceiptById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getReceiptsByOrganization(
            @RequestParam UUID organizationId) {
        List<ReceiptResponse> response = receiptService.getReceiptsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        byte[] pdfBytes = receiptService.getReceiptPdf(id, organizationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "receipt_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
