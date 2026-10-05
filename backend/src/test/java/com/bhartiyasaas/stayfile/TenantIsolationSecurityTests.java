package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.MeterReading;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.TicketCategory;
import com.bhartiyasaas.stayfile.entity.enums.TicketPriority;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.mapper.InvoiceMapper;
import com.bhartiyasaas.stayfile.mapper.MaintenanceTicketMapper;
import com.bhartiyasaas.stayfile.mapper.MeterReadingMapper;
import com.bhartiyasaas.stayfile.repository.InvoiceRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.MaintenanceTicketRepository;
import com.bhartiyasaas.stayfile.repository.MeterReadingRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.impl.InvoiceServiceImpl;
import com.bhartiyasaas.stayfile.service.impl.MaintenanceTicketServiceImpl;
import com.bhartiyasaas.stayfile.service.impl.MeterReadingServiceImpl;

@ExtendWith(MockitoExtension.class)
class TenantIsolationSecurityTests {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private MaintenanceTicketMapper maintenanceTicketMapper;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @Mock
    private MeterReadingMapper meterReadingMapper;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private TenantAccessService tenantAccessService;

    private UUID orgId;
    private Organization organization;
    private SecurityUser tenantUser;
    private Tenant callingTenant;
    private Unit foreignUnit;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("Test Org").build();
        Profile tenantProfile = Profile.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("tenant@test.com")
                .passwordHash("pass")
                .role(UserRole.TENANT)
                .isActive(true)
                .build();
        tenantUser = new SecurityUser(tenantProfile);

        callingTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .fullName("Calling Tenant")
                .user(tenantProfile)
                .build();

        foreignUnit = Unit.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .unitNumber("A-202")
                .build();
    }

    @Test
    void maintenanceTicket_tenantCannotCreateTicketForUnitTheyDoNotLease() {
        MaintenanceTicketServiceImpl ticketService = new MaintenanceTicketServiceImpl(
                maintenanceTicketRepository, organizationRepository, unitRepository,
                tenantRepository, leaseRepository, maintenanceTicketMapper, tenantAccessService
        );

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(unitRepository.findByIdAndOrganizationId(foreignUnit.getId(), orgId)).thenReturn(Optional.of(foreignUnit));
        when(tenantRepository.findByIdAndOrganizationId(callingTenant.getId(), orgId)).thenReturn(Optional.of(callingTenant));
        // Calling tenant has NO leases for foreignUnit
        when(leaseRepository.findByTenantId(callingTenant.getId())).thenReturn(Collections.emptyList());

        MaintenanceTicketCreateRequest request = MaintenanceTicketCreateRequest.builder()
                .unitId(foreignUnit.getId())
                .tenantId(callingTenant.getId())
                .title("Leaking sink")
                .category(TicketCategory.PLUMBING)
                .priority(TicketPriority.MEDIUM)
                .build();

        assertThatThrownBy(() -> ticketService.createTicket(request, tenantUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You can only submit maintenance tickets for units you are currently leasing");
    }

    @Test
    void meterReading_tenantCannotAccessReadingsForUnitTheyDoNotLease() {
        MeterReadingServiceImpl readingService = new MeterReadingServiceImpl(
                meterReadingRepository, unitRepository, organizationRepository, meterReadingMapper,
                leaseRepository, tenantRepository, tenantAccessService
        );

        MeterReading reading = MeterReading.builder()
                .id(UUID.randomUUID())
                .unit(foreignUnit)
                .organization(organization)
                .build();

        when(meterReadingRepository.findByIdAndOrganizationId(reading.getId(), orgId)).thenReturn(Optional.of(reading));
        when(tenantRepository.findByUserIdAndOrganizationId(tenantUser.getId(), orgId)).thenReturn(Optional.of(callingTenant));
        when(leaseRepository.findByTenantId(callingTenant.getId())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> readingService.getMeterReadingById(reading.getId(), tenantUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only view meter readings for your assigned unit");
    }

    @Test
    void getInvoicesForCurrentUser_returnsInvoicesScopedToCallingTenantProfile() {
        InvoiceServiceImpl invoiceService = new InvoiceServiceImpl(
                invoiceRepository, leaseRepository, organizationRepository,
                tenantRepository, unitRepository, meterReadingRepository,
                invoiceMapper, null, tenantAccessService
        );

        Invoice myInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .tenant(callingTenant)
                .totalAmount(new BigDecimal("5000.00"))
                .build();

        when(tenantRepository.findByUserIdAndOrganizationId(tenantUser.getId(), orgId)).thenReturn(Optional.of(callingTenant));
        when(invoiceRepository.findByTenantIdAndOrganizationId(callingTenant.getId(), orgId)).thenReturn(List.of(myInvoice));
        when(invoiceMapper.toResponseList(any())).thenReturn(List.of(InvoiceResponse.builder().id(myInvoice.getId()).build()));

        List<InvoiceResponse> responses = invoiceService.getInvoicesForCurrentUser(tenantUser);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(myInvoice.getId());
    }
}
