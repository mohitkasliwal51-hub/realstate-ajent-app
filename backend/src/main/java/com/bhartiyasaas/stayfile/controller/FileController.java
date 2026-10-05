package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.storage.StorageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import com.bhartiyasaas.stayfile.repository.InvoiceRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.ReceiptRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final TenantRepository tenantRepository;
    private final LeaseRepository leaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT', 'TENANT')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") FileCategory category,
            @AuthenticationPrincipal SecurityUser currentUser) {

        if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT
                && category != FileCategory.TENANT_KYC && category != FileCategory.SIGNATURE) {
            throw new AccessDeniedException("Tenants are only permitted to upload KYC documents or signatures");
        }

        FileUploadResponse response = storageService.storeFile(file, category, currentUser.getOrganizationId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }

    @GetMapping("/private/**")
    public ResponseEntity<Resource> downloadPrivateFile(
            HttpServletRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {

        String fullPath = request.getRequestURI();
        String key = fullPath.substring(fullPath.indexOf("/private/") + 9);

        if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
            validateTenantFileAccess(key, currentUser);
        }

        Resource resource = storageService.loadFileAsResource(key, currentUser.getOrganizationId());
        String contentType = StorageKeys.contentTypeOf(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private void validateTenantFileAccess(String key, SecurityUser currentUser) {
        var tenantOpt = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), currentUser.getOrganizationId());
        if (tenantOpt.isEmpty()) {
            throw new AccessDeniedException("Access denied: No tenant profile found");
        }
        var tenant = tenantOpt.get();
        if (tenant.getIdProofDocumentUrl() != null && tenant.getIdProofDocumentUrl().contains(key)) {
            return;
        }
        boolean matchesLease = leaseRepository.findByTenantId(tenant.getId()).stream()
                .anyMatch(l -> l.getAgreementPdfUrl() != null && l.getAgreementPdfUrl().contains(key));
        if (matchesLease) {
            return;
        }
        boolean matchesReceipt = receiptRepository.findByTenantId(tenant.getId()).stream()
                .anyMatch(r -> r.getPdfUrl() != null && r.getPdfUrl().contains(key));
        if (matchesReceipt) {
            return;
        }
        boolean matchesInvoice = invoiceRepository.findByTenantIdAndOrganizationId(tenant.getId(), currentUser.getOrganizationId()).stream()
                .anyMatch(inv -> inv.getInvoicePdfUrl() != null && inv.getInvoicePdfUrl().contains(key));
        if (matchesInvoice) {
            return;
        }
        throw new AccessDeniedException("Access denied: You can only access your own private files");
    }
}
