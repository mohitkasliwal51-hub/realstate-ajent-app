package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.ReceiptMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
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
    private final ReceiptMapper receiptMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public ReceiptResponse createReceipt(ReceiptCreateRequest request) {
        tenantAccessService.validateUserOrganization(request.getOrganizationId());

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + request.getLeaseId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        if (lease.getOrganization() == null || !lease.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Lease does not belong to the specified Organization");
        }
        if (tenant.getOrganization() == null || !tenant.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Tenant does not belong to the specified Organization");
        }
        if (owner.getOrganization() == null || !owner.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Owner profile does not belong to the specified Organization");
        }
        if (lease.getTenant() == null || !lease.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("The specified Tenant does not match the Tenant associated with the Lease");
        }

        String receiptNumber = String.format("REC-%s-%d-%s",
                organization.getSlug().toUpperCase(),
                LocalDate.now().getYear(),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Receipt receipt = receiptMapper.toEntity(request);
        receipt.setReceiptNumber(receiptNumber);
        receipt.setOrganization(organization);
        receipt.setLease(lease);
        receipt.setTenant(tenant);
        receipt.setOwner(owner);

        Receipt savedReceipt = receiptRepository.save(receipt);
        return receiptMapper.toResponse(savedReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(UUID id, UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(receipt.getTenant(), "payment receipt");
        return receiptMapper.toResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByOrganization(UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        List<Receipt> receipts = receiptRepository.findByOrganizationId(organizationId).stream()
                .filter(receipt -> tenantAccessService.canAccessTenant(receipt.getTenant(), "payment receipt"))
                .collect(Collectors.toList());

        return receiptMapper.toResponseList(receipts);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReceiptPdf(UUID receiptId, UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(receiptId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        tenantAccessService.validateTenantOwnership(receipt.getTenant(), "payment receipt");
        return pdfGeneratorService.generatePaymentReceiptPdf(receipt);
    }
}
