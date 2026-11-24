package com.bhartiyasaas.stayfile.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.InvoiceRequest;
import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.InvoiceLineItem;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.ChargeType;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceStatus;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.MaintenanceFeeType;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.InvoiceMapper;
import com.bhartiyasaas.stayfile.repository.InvoiceRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.InvoiceService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.bhartiyasaas.stayfile.service.PaymentGatewayProvider;
import com.bhartiyasaas.stayfile.dto.response.PaymentLinkResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantRepository tenantRepository;
    private final UnitRepository unitRepository;
    private final InvoiceMapper invoiceMapper;
    private final PdfGeneratorService pdfGeneratorService;
    private final TenantAccessService tenantAccessService;
    private final PaymentGatewayProvider paymentGatewayProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Lease lease = leaseRepository.findByIdAndOrganizationId(request.getLeaseId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + request.getLeaseId()));

        Tenant tenant = tenantRepository.findByIdAndOrganizationId(request.getTenantId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        Unit unit = unitRepository.findByIdAndOrganizationId(request.getUnitId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));
        if (!lease.getTenant().getId().equals(tenant.getId()) || !lease.getUnit().getId().equals(unit.getId())) {
            throw new BadRequestException("Invoice tenant and unit must match the lease");
        }

        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setOrganization(organization);
        invoice.setLease(lease);
        invoice.setTenant(tenant);
        invoice.setUnit(unit);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);

        List<InvoiceLineItem> items = new ArrayList<>();
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        if (request.getLineItems() != null) {
            for (InvoiceRequest.LineItemRequest itemReq : request.getLineItems()) {
                BigDecimal quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal amount = itemReq.getAmount() != null ? itemReq.getAmount() : quantity.multiply(unitPrice);
                if (quantity.signum() < 0 || unitPrice.signum() < 0 || amount.signum() < 0) {
                    throw new BadRequestException("Invoice line item values cannot be negative");
                }
                InvoiceLineItem item = new InvoiceLineItem();
                item.setOrganization(organization);
                item.setInvoice(invoice);
                item.setChargeType(itemReq.getChargeType() != null ? itemReq.getChargeType() : ChargeType.RENT);
                item.setDescription(itemReq.getDescription() != null ? itemReq.getDescription() : "Charge");
                item.setQuantity(quantity);
                item.setUnitPrice(unitPrice);
                item.setAmount(amount);
                calculatedSubtotal = calculatedSubtotal.add(amount);
                items.add(item);
            }
        }

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxTotal = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        if (discount.signum() < 0 || taxTotal.signum() < 0 || discount.compareTo(calculatedSubtotal) > 0) {
            throw new BadRequestException("Invalid invoice discount or tax amount");
        }
        BigDecimal total = calculatedSubtotal.add(taxTotal).subtract(discount);
        invoice.setSubtotalAmount(calculatedSubtotal);
        invoice.setTaxAmount(taxTotal);
        invoice.setDiscountAmount(discount);
        invoice.setTotalAmount(total);
        invoice.setLineItems(items);

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(invoice.getTenant(), "invoice");
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForCurrentUser(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Tenant tenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for current user"));
        List<Invoice> invoices = invoiceRepository.findByTenantIdAndOrganizationId(tenant.getId(), organizationId);
        return invoiceMapper.toResponseList(invoices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByLease(UUID leaseId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "invoice");
        return invoiceMapper.toResponseList(invoiceRepository.findByLeaseIdAndOrganizationId(leaseId, organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        return invoiceMapper.toResponseList(invoiceRepository.findByOrganizationId(organizationId).stream()
            .filter(invoice -> tenantAccessService.canAccessTenant(invoice.getTenant(), "invoice"))
            .toList());
    }

    @Override
    @Transactional
        public InvoiceResponse generateMoveInInvoice(UUID leaseId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));

        Invoice existing = invoiceRepository.findByLeaseIdAndOrganizationIdAndBillingPeriodStartAndInvoiceType(
            leaseId, organizationId, lease.getStartDate(), InvoiceType.MOVE_IN).orElse(null);
        if (existing != null) {
            return invoiceMapper.toResponse(existing);
        }

        Invoice invoice = new Invoice();
        invoice.setOrganization(organization);
        invoice.setLease(lease);
        invoice.setTenant(lease.getTenant());
        invoice.setUnit(lease.getUnit());
        invoice.setInvoiceNumber(createInvoiceNumber("MOVE"));
        invoice.setInvoiceType(InvoiceType.MOVE_IN);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setBillingPeriodStart(lease.getStartDate());
        invoice.setBillingPeriodEnd(lease.getStartDate());
        invoice.setDueDate(lease.getStartDate());
        invoice.setPaidAmount(BigDecimal.ZERO);

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        subtotal = addLine(lineItems, organization, invoice, ChargeType.RENT, "Move-in rent", lease.getMonthlyRent(), subtotal);
        subtotal = addLine(lineItems, organization, invoice, ChargeType.SECURITY_DEPOSIT, "Refundable security deposit", lease.getSecurityDeposit(), subtotal);
        if (lease.getBrokerageFeeType() == com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType.ONE_TIME) {
            subtotal = addLine(lineItems, organization, invoice, ChargeType.ONE_TIME_BROKERAGE,
                "One-time brokerage fee", lease.getBrokerageAmount(), subtotal);
        }
        if (lease.getMaintenanceFeeType() == MaintenanceFeeType.ANNUAL_ONE_TIME) {
            subtotal = addLine(lineItems, organization, invoice, ChargeType.MAINTENANCE_FEE,
                "Annual maintenance fee", lease.getMaintenanceFeeAmount(), subtotal);
        }
        subtotal = addLine(lineItems, organization, invoice, ChargeType.AGREEMENT_FEE,
            "E-Stamp and e-Sign agreement fee", lease.getAgreementFeeAmount(), subtotal);

        invoice.setSubtotalAmount(subtotal);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(subtotal);
        invoice.setLineItems(lineItems);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
        }

    @Override
    @Transactional
    public List<InvoiceResponse> generateMonthlyInvoicesForOrganization(SecurityUser currentUser) {
        return generateMonthlyInvoicesForOrgId(currentUser.getOrganizationId());
    }

    private List<InvoiceResponse> generateMonthlyInvoicesForOrgId(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId).orElse(null);
        if (organization == null) {
            return Collections.emptyList();
        }

        List<Lease> activeLeases = leaseRepository.findByOrganizationId(organizationId).stream()
                .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<Invoice> generatedInvoices = new ArrayList<>();

        for (Lease lease : activeLeases) {
                Invoice existing = invoiceRepository.findByLeaseIdAndOrganizationIdAndBillingPeriodStartAndInvoiceType(
                    lease.getId(), organizationId, startOfMonth, InvoiceType.MONTHLY_RENT).orElse(null);
            if (existing != null) {
                generatedInvoices.add(existing);
                continue;
            }

            Invoice invoice = new Invoice();
            invoice.setOrganization(organization);
            invoice.setLease(lease);
            invoice.setTenant(lease.getTenant());
            invoice.setUnit(lease.getUnit());
            invoice.setInvoiceNumber(createInvoiceNumber("MONTHLY"));
            invoice.setInvoiceType(InvoiceType.MONTHLY_RENT);
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice.setBillingPeriodStart(startOfMonth);
            invoice.setBillingPeriodEnd(endOfMonth);
            invoice.setDueDate(startOfMonth.withDayOfMonth(Math.min(lease.getRentDueDay() != null ? lease.getRentDueDay() : 5, endOfMonth.getDayOfMonth())));
            invoice.setPaidAmount(BigDecimal.ZERO);

            List<InvoiceLineItem> lineItems = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;

            // 1. Rent Line Item
            InvoiceLineItem rentItem = new InvoiceLineItem();
            rentItem.setOrganization(organization);
            rentItem.setInvoice(invoice);
            rentItem.setChargeType(ChargeType.RENT);
            rentItem.setDescription("Monthly Rent - " + startOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            rentItem.setQuantity(BigDecimal.ONE);
            rentItem.setUnitPrice(lease.getMonthlyRent());
            rentItem.setAmount(lease.getMonthlyRent());
            lineItems.add(rentItem);
            subtotal = subtotal.add(lease.getMonthlyRent());

            // 2. Maintenance Fee (if recurring monthly)
                if (lease.getMaintenanceFeeType() == MaintenanceFeeType.MONTHLY
                    && lease.getMaintenanceFeeAmount() != null && lease.getMaintenanceFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
                InvoiceLineItem maintItem = new InvoiceLineItem();
                maintItem.setOrganization(organization);
                maintItem.setInvoice(invoice);
                maintItem.setChargeType(ChargeType.MAINTENANCE_FEE);
                maintItem.setDescription("Monthly Maintenance Fee");
                maintItem.setQuantity(BigDecimal.ONE);
                maintItem.setUnitPrice(lease.getMaintenanceFeeAmount());
                maintItem.setAmount(lease.getMaintenanceFeeAmount());
                lineItems.add(maintItem);
                subtotal = subtotal.add(lease.getMaintenanceFeeAmount());
            }

                if (lease.getBrokerageFeeType() == com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType.RECURRING_PERCENTAGE
                    && lease.getBrokerageAmount() != null) {
                BigDecimal commission = lease.getMonthlyRent().multiply(lease.getBrokerageAmount())
                    .divide(BigDecimal.valueOf(100));
                subtotal = addLine(lineItems, organization, invoice, ChargeType.RECURRING_COMMISSION,
                    "Recurring agency commission", commission, subtotal);
                } else if (lease.getBrokerageFeeType() == com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType.RECURRING_FIXED) {
                subtotal = addLine(lineItems, organization, invoice, ChargeType.RECURRING_COMMISSION,
                    "Recurring agency commission", lease.getBrokerageAmount(), subtotal);
            }

            invoice.setSubtotalAmount(subtotal);
            invoice.setTaxAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(subtotal);
            invoice.setLineItems(lineItems);

            Invoice saved = invoiceRepository.save(invoice);
            generatedInvoices.add(saved);
        }

        return invoiceMapper.toResponseList(generatedInvoices);
    }

    private BigDecimal addLine(List<InvoiceLineItem> lineItems, Organization organization, Invoice invoice,
                               ChargeType chargeType, String description, BigDecimal amount, BigDecimal subtotal) {
        if (amount == null || amount.signum() <= 0) {
            return subtotal;
        }
        InvoiceLineItem item = new InvoiceLineItem();
        item.setOrganization(organization);
        item.setInvoice(invoice);
        item.setChargeType(chargeType);
        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(amount);
        item.setAmount(amount);
        lineItems.add(item);
        return subtotal.add(amount);
    }

    private String createInvoiceNumber(String type) {
        return "INV-" + type + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadInvoicePdf(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(invoice.getTenant(), "invoice");
        return pdfGeneratorService.generateInvoicePdf(invoice);
    }

    @Override
    @Transactional
    public PaymentLinkResponse getOrCreatePaymentLink(UUID invoiceId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + invoiceId));
        tenantAccessService.validateTenantOwnership(invoice.getTenant(), "invoice");

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Invoice is already fully paid");
        }

        String cachedUrl = null;
        if (invoice.getMetadata() != null && !invoice.getMetadata().isBlank()) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                        invoice.getMetadata(), new TypeReference<Map<String, Object>>() {});
                Object linkStatus = metadata.get("link_status");
                if (!"EXPIRED".equalsIgnoreCase(String.valueOf(linkStatus))) {
                    Object url = metadata.get("link_url");
                    if (url != null) cachedUrl = String.valueOf(url);
                }
            } catch (Exception ex) {
                cachedUrl = null;
            }
        }

        if (cachedUrl == null) {
            Tenant tenant = invoice.getTenant();
            BigDecimal balance = invoice.getBalanceDue() != null
                    ? invoice.getBalanceDue()
                    : invoice.getTotalAmount().subtract(
                            invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO);
            cachedUrl = paymentGatewayProvider.createPaymentLink(
                    invoice.getId(), balance, tenant.getFullName(), tenant.getPhone(),
                    tenant.getEmail(), "Payment for invoice " + invoice.getInvoiceNumber());

            try {
                Map<String, Object> metadata = new java.util.LinkedHashMap<>();
                metadata.put("cf_link_id", "INV_" + invoice.getId());
                metadata.put("link_id", "INV_" + invoice.getId());
                metadata.put("link_url", cachedUrl);
                metadata.put("link_status", "ACTIVE");
                metadata.put("link_amount", balance);
                metadata.put("expiry_time", null);
                invoice.setMetadata(objectMapper.writeValueAsString(metadata));
                invoiceRepository.save(invoice);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to cache payment link metadata", ex);
            }
        }

        return PaymentLinkResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getBalanceDue() != null ? invoice.getBalanceDue() : invoice.getTotalAmount())
                .paymentLinkUrl(cachedUrl)
                .status(invoice.getStatus())
                .build();
    }

    @Override
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Kolkata")
    @Transactional
    public void processOverdueInvoicesScheduled() {
        invoiceRepository.updateOverdueInvoices(
            LocalDate.now(), 
            InvoiceStatus.OVERDUE, 
            InvoiceStatus.UNPAID, 
            InvoiceStatus.PARTIAL
        );
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 1 * ?", zone = "Asia/Kolkata")
    @Transactional
    public void generateMonthlyInvoicesScheduled() {
        List<Organization> orgs = organizationRepository.findAll();
        for (Organization org : orgs) {
            try {
                generateMonthlyInvoicesForOrgId(org.getId());
            } catch (Exception e) {
                // Log and continue for next organization
            }
        }
    }
}

