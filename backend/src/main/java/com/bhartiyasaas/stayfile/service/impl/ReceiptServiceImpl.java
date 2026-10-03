package com.bhartiyasaas.stayfile.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Receipt;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.ReceiptMapper;
import com.bhartiyasaas.stayfile.repository.InvoiceRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ReceiptRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.bhartiyasaas.stayfile.service.ReceiptService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OrganizationRepository organizationRepository;
    private final LeaseRepository leaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final ReceiptMapper receiptMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public ReceiptResponse createReceipt(ReceiptCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Lease lease = leaseRepository.findByIdAndOrganizationId(request.getLeaseId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + request.getLeaseId()));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("Receipts can only be issued for active leases");
        }
        if (!lease.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("Receipt tenant does not match the lease");
        }

        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndOrganizationId(request.getInvoiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + request.getInvoiceId()));
            if (!invoice.getLease().getId().equals(lease.getId())
                    || !invoice.getTenant().getId().equals(lease.getTenant().getId())) {
                throw new IllegalArgumentException("Invoice does not belong to the selected lease and tenant");
            }
        }

        String prefix = "REC-" + LocalDate.now().getYear() + "-";
        int nextSequence = receiptRepository
                .findTopByOrganizationIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(organization.getId(), prefix)
                .map(receipt -> Integer.parseInt(receipt.getReceiptNumber().substring(prefix.length())) + 1)
                .orElse(1);
        String receiptNumber = prefix + String.format("%06d", nextSequence);

        Receipt receipt = receiptMapper.toEntity(request);
        receipt.setReceiptNumber(receiptNumber);
        receipt.setOrganization(organization);
        receipt.setInvoice(invoice);
        receipt.setLease(lease);
        receipt.setTenant(lease.getTenant());

        Receipt savedReceipt = receiptRepository.save(receipt);
        return receiptMapper.toResponse(savedReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(receipt.getTenant(), "payment receipt");
        return receiptMapper.toResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsForCurrentUser(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        var tenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for current user"));
        List<Receipt> receipts = receiptRepository.findByTenantId(tenant.getId());
        return receiptMapper.toResponseList(receipts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        List<Receipt> receipts = receiptRepository.findByOrganizationId(organizationId).stream()
                .filter(receipt -> tenantAccessService.canAccessTenant(receipt.getTenant(), "payment receipt"))
                .collect(Collectors.toList());

        return receiptMapper.toResponseList(receipts);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReceiptPdf(UUID receiptId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Receipt receipt = receiptRepository.findByIdAndOrganizationId(receiptId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        tenantAccessService.validateTenantOwnership(receipt.getTenant(), "payment receipt");
        return pdfGeneratorService.generatePaymentReceiptPdf(receipt);
    }
}
