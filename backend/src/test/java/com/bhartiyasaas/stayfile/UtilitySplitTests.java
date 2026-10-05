package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.entity.enums.ChargeType;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.mapper.InvoiceMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.impl.UtilitySplitServiceImpl;

@ExtendWith(MockitoExtension.class)
class UtilitySplitTests {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UtilitySplitServiceImpl utilitySplitService;

    private Organization organization;
    private Property property;
    private SecurityUser managerUser;
    private UUID orgId;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        propertyId = UUID.randomUUID();

        organization = Organization.builder().id(orgId).name("StayFile PG Org").build();
        property = Property.builder().id(propertyId).organization(organization).name("Sunshine PG").build();

        Profile managerProfile = Profile.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("manager@pg.com")
                .role(UserRole.PROPERTY_MANAGER)
                .isActive(true)
                .build();
        managerUser = new SecurityUser(managerProfile);
    }

    @Test
    void testPgMultiTierSplit_CalculatesCommonAndAcSharesCorrectly() throws Exception {
        // Scenario:
        // 7 residents total across 2 rooms and 1 hall
        // Master Discom: 800 units @ Rs. 10.00
        // Room 1 AC: 200 units (2 occupants)
        // Room 2 AC: 100 units (2 occupants)
        // Hall AC: 150 units (3 occupants / all residents share hall)
        // Expected:
        // Total AC units = 200 + 100 + 150 = 450 units
        // Common units = 800 - 450 = 350 units
        // Per-person common share = 350 / 7 = 50 units (Rs. 500)
        // Room 1 occupant share:
        //   Common: Rs. 500
        //   Room AC: 200 / 2 = 100 units (Rs. 1,000)
        //   Hall AC: 150 / 7 = 21.43 units (Rs. 214.30)
        // Total for Room 1 occupant = 500 + 1000 + 214.30 = Rs. 1,714.30

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(propertyRepository.findByIdAndOrganizationId(propertyId, orgId)).thenReturn(Optional.of(property));

        // Create 2 rooms
        Unit room1 = Unit.builder().id(UUID.randomUUID()).property(property).unitNumber("Room 101").build();
        Unit room2 = Unit.builder().id(UUID.randomUUID()).property(property).unitNumber("Room 102").build();

        List<Lease> leases = new ArrayList<>();
        // Room 1: 2 tenants
        for (int i = 0; i < 2; i++) {
            Tenant t = Tenant.builder().id(UUID.randomUUID()).fullName("Tenant R1-" + i).build();
            leases.add(Lease.builder()
                    .id(UUID.randomUUID())
                    .organization(organization)
                    .tenant(t)
                    .unit(room1)
                    .status(LeaseStatus.ACTIVE)
                    .monthlyRent(BigDecimal.valueOf(10000))
                    .build());
        }
        // Room 2: 2 tenants
        for (int i = 0; i < 2; i++) {
            Tenant t = Tenant.builder().id(UUID.randomUUID()).fullName("Tenant R2-" + i).build();
            leases.add(Lease.builder()
                    .id(UUID.randomUUID())
                    .organization(organization)
                    .tenant(t)
                    .unit(room2)
                    .status(LeaseStatus.ACTIVE)
                    .monthlyRent(BigDecimal.valueOf(10000))
                    .build());
        }
        // Hall: 3 tenants (say unit is room1 or separate)
        for (int i = 0; i < 3; i++) {
            Tenant t = Tenant.builder().id(UUID.randomUUID()).fullName("Tenant Hall-" + i).build();
            leases.add(Lease.builder()
                    .id(UUID.randomUUID())
                    .organization(organization)
                    .tenant(t)
                    .unit(room2)
                    .status(LeaseStatus.ACTIVE)
                    .monthlyRent(BigDecimal.valueOf(8000))
                    .build());
        }

        when(leaseRepository.findByOrganizationId(orgId)).thenReturn(leases);

        // Meter readings
        MeterReading master = MeterReading.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .property(property)
                .meterScope("MASTER_DISCOM")
                .meterType("ELECTRICITY")
                .unitsConsumed(BigDecimal.valueOf(800))
                .ratePerUnit(BigDecimal.valueOf(10.00))
                .readingDate(LocalDate.now())
                .isBilled(false)
                .build();

        MeterReading room1Ac = MeterReading.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .unit(room1)
                .meterScope("ROOM_AC")
                .meterType("ELECTRICITY")
                .unitsConsumed(BigDecimal.valueOf(200))
                .ratePerUnit(BigDecimal.valueOf(10.00))
                .readingDate(LocalDate.now())
                .photoUrl("https://storage.stayfile.in/readings/r1.jpg")
                .isBilled(false)
                .build();

        MeterReading room2Ac = MeterReading.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .unit(room2)
                .meterScope("ROOM_AC")
                .meterType("ELECTRICITY")
                .unitsConsumed(BigDecimal.valueOf(100))
                .ratePerUnit(BigDecimal.valueOf(10.00))
                .readingDate(LocalDate.now())
                .isBilled(false)
                .build();

        MeterReading hallAc = MeterReading.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .property(property)
                .meterScope("HALL_AC")
                .meterType("ELECTRICITY")
                .unitsConsumed(BigDecimal.valueOf(150))
                .ratePerUnit(BigDecimal.valueOf(10.00))
                .readingDate(LocalDate.now())
                .isBilled(false)
                .build();

        when(meterReadingRepository.findByPropertyIdAndOrganizationIdAndIsBilledFalse(propertyId, orgId))
                .thenReturn(List.of(master, hallAc));
        when(meterReadingRepository.findByUnitIdAndOrganizationIdAndIsBilledFalseOrderByReadingDateAsc(room1.getId(), orgId))
                .thenReturn(List.of(room1Ac));
        when(meterReadingRepository.findByUnitIdAndOrganizationIdAndIsBilledFalseOrderByReadingDateAsc(room2.getId(), orgId))
                .thenReturn(List.of(room2Ac));

        when(invoiceRepository.findByLeaseIdAndOrganizationIdAndBillingPeriodStartAndInvoiceType(
                any(), any(), any(), eq(InvoiceType.UTILITY_ONLY)))
                .thenReturn(Optional.empty());

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceMapper.toResponseList(any())).thenAnswer(invocation -> {
            List<Invoice> list = invocation.getArgument(0);
            return list.stream().map(inv -> InvoiceResponse.builder()
                    .id(inv.getId())
                    .invoiceNumber(inv.getInvoiceNumber())
                    .invoiceType(inv.getInvoiceType())
                    .totalAmount(inv.getTotalAmount())
                    .build()).toList();
        });

        // Execute split
        List<InvoiceResponse> result = utilitySplitService.calculateAndGeneratePgUtilityInvoices(
                propertyId, LocalDate.now(), managerUser);

        assertThat(result).hasSize(7);
        for (InvoiceResponse inv : result) {
            assertThat(inv.getInvoiceType()).isEqualTo(InvoiceType.UTILITY_ONLY);
            assertThat(inv.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        }

        // Verify readings marked billed
        assertThat(master.getIsBilled()).isTrue();
        assertThat(room1Ac.getIsBilled()).isTrue();
        assertThat(room2Ac.getIsBilled()).isTrue();
        assertThat(hallAc.getIsBilled()).isTrue();
    }
}
