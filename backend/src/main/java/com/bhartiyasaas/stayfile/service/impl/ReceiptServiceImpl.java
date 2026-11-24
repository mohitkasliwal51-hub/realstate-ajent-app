package com.bhartiyasaas.stayfile.service.impl;

import java.math.BigDecimal;
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
import com.bhartiyasaas.stayfile.entity.enums.InvoiceStatus;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
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

        if (lease.getStatus() == LeaseStatus.TERMINATED || lease.getStatus() == LeaseStatus.CANCELLED || lease.getStatus() == LeaseStatus.EXPIRED) {
            throw new IllegalStateException("Receipts cannot be issued for terminated, cancelled, or expired leases");
        }
        if (!lease.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("Receipt tenant does not match the lease");
        }

        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            if (receiptRepository.existsByOrganizationIdAndTransactionReference(organizationId, request.getTransactionReference().trim())) {
                throw new com.bhartiyasaas.stayfile.exception.BadRequestException("A receipt with transaction reference '" + request.getTransactionReference() + "' already exists");
            }
        }

        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndOrganizationId(request.getInvoiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + request.getInvoiceId()));
            if (!invoice.getLease().getId().equals(lease.getId())
                    || !invoice.getTenant().getId().equals(lease.getTenant().getId())) {
                throw new IllegalArgumentException("Invoice does not belong to the selected lease and tenant");
            }
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                throw new com.bhartiyasaas.stayfile.exception.BadRequestException("Invoice is already fully paid");
            }
            BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balanceDue = invoice.getTotalAmount().subtract(currentPaid);
            if (request.getAmount().compareTo(balanceDue) > 0) {
                throw new com.bhartiyasaas.stayfile.exception.BadRequestException(
                    "Receipt amount (" + request.getAmount() + ") exceeds remaining invoice balance due (" + balanceDue + ")");
            }

            BigDecimal newPaid = currentPaid.add(request.getAmount());
            invoice.setPaidAmount(newPaid);
            if (newPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.PARTIAL);
            }
            invoiceRepository.save(invoice);
        }

        String prefix = "REC-" + LocalDate.now().getYear() + "-";
        int nextSequence = receiptRepository
                .findTopByOrganizationIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(organization.getId(), prefix)
                .map(r -> Integer.parseInt(r.getReceiptNumber().substring(prefix.length())) + 1)
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
