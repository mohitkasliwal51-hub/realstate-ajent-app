package com.bhartiyasaas.stayfile.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bhartiyasaas.stayfile.dto.request.InvoiceRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.InvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        InvoiceResponse response = invoiceService.createInvoice(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invoice created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        InvoiceResponse response = invoiceService.getInvoiceById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByLease(
            @PathVariable UUID leaseId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<InvoiceResponse> response = invoiceService.getInvoicesByLease(leaseId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<InvoiceResponse> response = invoiceService.getInvoicesByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/move-in/{leaseId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateMoveInInvoice(
            @PathVariable UUID leaseId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        InvoiceResponse response = invoiceService.generateMoveInInvoice(leaseId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Move-in invoice generated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER')")
    @PostMapping("/generate-monthly")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> generateMonthlyInvoices(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<InvoiceResponse> response = invoiceService.generateMonthlyInvoicesForOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Monthly itemized invoices generated successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser) {
        byte[] pdfBytes = invoiceService.downloadInvoicePdf(id, currentUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "invoice-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
