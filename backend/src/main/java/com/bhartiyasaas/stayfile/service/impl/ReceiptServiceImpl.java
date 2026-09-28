package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.bhartiyasaas.stayfile.service.ReceiptService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrganizationRepository organizationRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final ProfileRepository profileRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    @Transactional
    public ReceiptResponse createReceipt(ReceiptCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + request.getLeaseId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        // Format receipt number: REC-{ORG_SLUG}-{YYYY}-{COUNT+1}
        long count = receiptRepository.findByOrganizationId(organization.getId()).size() + 1;
        String receiptNumber = String.format("REC-%s-%d-%06d",
                organization.getSlug().toUpperCase(),
                LocalDate.now().getYear(),
                count);

        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .organization(organization)
                .lease(lease)
                .tenant(tenant)
                .owner(owner)
                .type(request.getType())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .transactionRef(request.getTransactionRef())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .notes(request.getNotes())
                .build();

        Receipt savedReceipt = receiptRepository.save(receipt);
        return mapToResponse(savedReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(UUID id, UUID organizationId) {
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + id));
        return mapToResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByOrganization(UUID organizationId) {
        return receiptRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReceiptPdf(UUID receiptId, UUID organizationId) {
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(receiptId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        return pdfGeneratorService.generatePaymentReceiptPdf(receipt);
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .organizationId(receipt.getOrganization().getId())
                .leaseId(receipt.getLease().getId())
                .tenantId(receipt.getTenant().getId())
                .tenantName(receipt.getTenant().getFullName())
                .ownerId(receipt.getOwner().getId())
                .type(receipt.getType())
                .amount(receipt.getAmount())
                .paymentMode(receipt.getPaymentMode())
                .transactionRef(receipt.getTransactionRef())
                .paymentDate(receipt.getPaymentDate())
                .periodStart(receipt.getPeriodStart())
                .periodEnd(receipt.getPeriodEnd())
                .notes(receipt.getNotes())
                .receiptPdfUrl(receipt.getReceiptPdfUrl())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .build();
    }
}
