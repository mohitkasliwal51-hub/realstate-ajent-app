package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Receipt;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceStatus;
import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.entity.enums.ReceiptType;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.mapper.ReceiptMapper;
import com.bhartiyasaas.stayfile.repository.InvoiceRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ReceiptRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.security.OrganizationPolicyService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.bhartiyasaas.stayfile.service.impl.ReceiptServiceImpl;

@ExtendWith(MockitoExtension.class)
class InvoiceAndAccountingTests {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private ReceiptMapper receiptMapper;
    @Mock
    private PdfGeneratorService pdfGeneratorService;
    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private OrganizationPolicyService organizationPolicyService;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    private UUID orgId;
    private Organization organization;
    private SecurityUser adminUser;
    private Invoice testInvoice;
    private Lease testLease;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("Test Org").build();
        Profile adminProfile = Profile.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("admin@test.com")
                .passwordHash("pass")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        adminUser = new SecurityUser(adminProfile);

        testTenant = Tenant.builder().id(UUID.randomUUID()).organization(organization).fullName("John Doe").build();
        testLease = Lease.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .tenant(testTenant)
                .status(com.bhartiyasaas.stayfile.entity.enums.LeaseStatus.ACTIVE)
                .build();
        testInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .tenant(testTenant)
                .lease(testLease)
                .totalAmount(new BigDecimal("10000.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.UNPAID)
                .build();
    }

    @Test
    void createReceipt_rejectsOverpaymentExceedingBalanceDue() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(leaseRepository.findByIdAndOrganizationId(testLease.getId(), orgId)).thenReturn(Optional.of(testLease));
        when(invoiceRepository.findByIdAndOrganizationId(testInvoice.getId(), orgId)).thenReturn(Optional.of(testInvoice));

        ReceiptCreateRequest request = ReceiptCreateRequest.builder()
                .leaseId(testLease.getId())
                .tenantId(testTenant.getId())
                .invoiceId(testInvoice.getId())
                .amount(new BigDecimal("15000.00")) // Exceeds balance due of 10000
                .paymentMode(PaymentMode.UPI)
                .receiptType(ReceiptType.RENT_PAYMENT)
                .build();

        assertThatThrownBy(() -> receiptService.createReceipt(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds remaining invoice balance due");
    }

    @Test
    void createReceipt_rejectsPaymentOnAlreadyPaidInvoice() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        testInvoice.setPaidAmount(new BigDecimal("10000.00"));

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(leaseRepository.findByIdAndOrganizationId(testLease.getId(), orgId)).thenReturn(Optional.of(testLease));
        when(invoiceRepository.findByIdAndOrganizationId(testInvoice.getId(), orgId)).thenReturn(Optional.of(testInvoice));

        ReceiptCreateRequest request = ReceiptCreateRequest.builder()
                .leaseId(testLease.getId())
                .tenantId(testTenant.getId())
                .invoiceId(testInvoice.getId())
                .amount(new BigDecimal("1000.00"))
                .paymentMode(PaymentMode.UPI)
                .receiptType(ReceiptType.RENT_PAYMENT)
                .build();

        assertThatThrownBy(() -> receiptService.createReceipt(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already fully paid");
    }

    @Test
    void createReceipt_rejectsDuplicateTransactionReferenceInSameOrg() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(leaseRepository.findByIdAndOrganizationId(testLease.getId(), orgId)).thenReturn(Optional.of(testLease));
        when(receiptRepository.existsByOrganizationIdAndTransactionReference(orgId, "TXN_DUP_123")).thenReturn(true);

        ReceiptCreateRequest request = ReceiptCreateRequest.builder()
                .leaseId(testLease.getId())
                .tenantId(testTenant.getId())
                .invoiceId(testInvoice.getId())
                .amount(new BigDecimal("5000.00"))
                .paymentMode(PaymentMode.NET_BANKING)
                .receiptType(ReceiptType.RENT_PAYMENT)
                .transactionReference("TXN_DUP_123")
                .build();

        assertThatThrownBy(() -> receiptService.createReceipt(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A receipt with transaction reference 'TXN_DUP_123' already exists");
    }

    @Test
    void createReceipt_correctlyUpdatesInvoiceStateOnPartialAndFullPayment() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(leaseRepository.findByIdAndOrganizationId(testLease.getId(), orgId)).thenReturn(Optional.of(testLease));
        when(invoiceRepository.findByIdAndOrganizationId(testInvoice.getId(), orgId)).thenReturn(Optional.of(testInvoice));
        when(receiptMapper.toEntity(any())).thenAnswer(invocation -> {
            ReceiptCreateRequest req = invocation.getArgument(0);
            return Receipt.builder()
                    .amount(req.getAmount())
                    .paymentMode(req.getPaymentMode())
                    .receiptType(req.getReceiptType())
                    .build();
        });
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptMapper.toResponse(any(Receipt.class))).thenReturn(new ReceiptResponse());

        ReceiptCreateRequest request = ReceiptCreateRequest.builder()
                .leaseId(testLease.getId())
                .tenantId(testTenant.getId())
                .invoiceId(testInvoice.getId())
                .amount(new BigDecimal("4000.00"))
                .paymentMode(PaymentMode.UPI)
                .receiptType(ReceiptType.RENT_PAYMENT)
                .build();

        receiptService.createReceipt(request, adminUser);

        // Verify invoice was updated to PARTIAL with 4000 paidAmount
        assertThat(testInvoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        verify(invoiceRepository).save(testInvoice);
    }
}
