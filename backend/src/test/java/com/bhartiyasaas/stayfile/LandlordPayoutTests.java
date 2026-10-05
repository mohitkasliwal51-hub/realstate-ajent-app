package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.bhartiyasaas.stayfile.dto.request.LandlordPayoutRequest;
import com.bhartiyasaas.stayfile.dto.request.PayoutStatusUpdateRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordPayoutResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.LandlordPayout;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.enums.PayoutStatus;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.mapper.LandlordPayoutMapper;
import com.bhartiyasaas.stayfile.repository.LandlordPayoutRepository;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.repository.ReceiptRepository;
import com.bhartiyasaas.stayfile.security.OrganizationPolicyService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.bhartiyasaas.stayfile.service.impl.LandlordPayoutServiceImpl;

@ExtendWith(MockitoExtension.class)
class LandlordPayoutTests {

    @Mock
    private LandlordPayoutRepository landlordPayoutRepository;
    @Mock
    private LandlordRepository landlordRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private LandlordPayoutMapper landlordPayoutMapper;
    @Mock
    private PdfGeneratorService pdfGeneratorService;
    @Mock
    private OrganizationPolicyService organizationPolicyService;

    @InjectMocks
    private LandlordPayoutServiceImpl landlordPayoutService;

    private UUID orgId;
    private Organization organization;
    private SecurityUser adminUser;
    private Landlord testLandlord;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("Test Org").build();
        com.bhartiyasaas.stayfile.entity.Profile adminProfile = com.bhartiyasaas.stayfile.entity.Profile.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("admin@test.com")
                .passwordHash("pass")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        adminUser = new SecurityUser(adminProfile);
        testLandlord = Landlord.builder().id(UUID.randomUUID()).managingOrganization(organization).legalName("Rajesh Sharma").build();
    }

    @Test
    void createPayout_rejectsDeductionsExceedingTotalCollected() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(landlordRepository.findByIdAndManagingOrganizationId(testLandlord.getId(), orgId)).thenReturn(Optional.of(testLandlord));
        when(landlordPayoutMapper.toEntity(any())).thenReturn(new LandlordPayout());

        LandlordPayoutRequest request = LandlordPayoutRequest.builder()
                .landlordId(testLandlord.getId())
                .totalCollected(new BigDecimal("10000.00"))
                .commissionAmount(new BigDecimal("6000.00"))
                .deductionsAmount(new BigDecimal("5000.00")) // 6000 + 5000 = 11000 > 10000
                .build();

        assertThatThrownBy(() -> landlordPayoutService.createPayout(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed total collected");
    }

    @Test
    void createPayout_rejectsDuplicatePeriodForSameLandlord() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(landlordRepository.findByIdAndManagingOrganizationId(testLandlord.getId(), orgId)).thenReturn(Optional.of(testLandlord));
        when(landlordPayoutMapper.toEntity(any())).thenReturn(new LandlordPayout());
        when(landlordPayoutRepository.findByOrganizationIdAndLandlordIdAndPeriodMonth(orgId, testLandlord.getId(), "2026-10"))
                .thenReturn(Optional.of(new LandlordPayout()));

        LandlordPayoutRequest request = LandlordPayoutRequest.builder()
                .landlordId(testLandlord.getId())
                .totalCollected(new BigDecimal("10000.00"))
                .commissionAmount(new BigDecimal("1000.00"))
                .deductionsAmount(new BigDecimal("500.00"))
                .periodMonth("2026-10")
                .build();

        assertThatThrownBy(() -> landlordPayoutService.createPayout(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A payout has already been created for landlord Rajesh Sharma for period 2026-10");
    }

    @Test
    void calculateMonthlyPayout_rejectsDuplicateWhenAlreadyGenerated() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(landlordRepository.findByIdAndManagingOrganizationId(testLandlord.getId(), orgId)).thenReturn(Optional.of(testLandlord));
        when(landlordPayoutRepository.findByOrganizationIdAndLandlordIdAndPeriodMonth(orgId, testLandlord.getId(), "2026-09"))
                .thenReturn(Optional.of(new LandlordPayout()));

        assertThatThrownBy(() -> landlordPayoutService.calculateMonthlyPayoutForLandlord(testLandlord.getId(), "2026-09", adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A payout has already been generated");
    }

    @Test
    void updatePayoutStatus_updatesStatusToSettledWithUtrAndDate() {
        LandlordPayout existing = LandlordPayout.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .landlord(testLandlord)
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        when(landlordPayoutRepository.findByIdAndOrganizationId(existing.getId(), orgId)).thenReturn(Optional.of(existing));
        when(landlordPayoutRepository.save(any(LandlordPayout.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landlordPayoutMapper.toResponse(any(LandlordPayout.class))).thenAnswer(invocation -> {
            LandlordPayout p = invocation.getArgument(0);
            return LandlordPayoutResponse.builder()
                    .id(p.getId())
                    .payoutStatus(p.getPayoutStatus())
                    .utrNumber(p.getUtrNumber())
                    .payoutDate(p.getPayoutDate())
                    .build();
        });

        PayoutStatusUpdateRequest updateRequest = PayoutStatusUpdateRequest.builder()
                .payoutStatus(PayoutStatus.SETTLED)
                .utrNumber("UTR123456789")
                .payoutDate(LocalDate.of(2026, 10, 5))
                .build();

        LandlordPayoutResponse response = landlordPayoutService.updatePayoutStatus(existing.getId(), updateRequest, adminUser);

        assertThat(response.getPayoutStatus()).isEqualTo(PayoutStatus.SETTLED);
        assertThat(response.getUtrNumber()).isEqualTo("UTR123456789");
        assertThat(response.getPayoutDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void updatePayoutStatus_rejectsTransitionFromSettled() {
        LandlordPayout settled = LandlordPayout.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .payoutStatus(PayoutStatus.SETTLED)
                .build();

        when(landlordPayoutRepository.findByIdAndOrganizationId(settled.getId(), orgId)).thenReturn(Optional.of(settled));

        PayoutStatusUpdateRequest updateRequest = PayoutStatusUpdateRequest.builder()
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        assertThatThrownBy(() -> landlordPayoutService.updatePayoutStatus(settled.getId(), updateRequest, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Settled payouts cannot transition back");
    }
}
