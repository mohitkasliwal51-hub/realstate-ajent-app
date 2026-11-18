package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.PanVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.PanVerificationResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import com.bhartiyasaas.stayfile.exception.IntegrationException;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePanResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePanVerifyRequest;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.impl.KycVerificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class KycPanVerificationTests {

    @Mock
    private CashfreeHttpClient cashfreeHttpClient;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LandlordRepository landlordRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private StorageService storageService;

    @Mock
    private RestClient restClient;

    @Spy
    private PiiEncryptionService piiEncryptionService = new PiiEncryptionService("12345678901234567890123456789012");

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private KycVerificationServiceImpl kycVerificationService;

    private UUID orgId;
    private Organization organization;
    private SecurityUser adminUser;
    private SecurityUser tenantUser;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("StayFile PM Org").build();

        Profile adminProfile = Profile.builder()
                .id(UUID.randomUUID())
                .email("admin@stayfile.in")
                .role(UserRole.ADMIN)
                .organization(organization)
                .build();
        adminUser = new SecurityUser(adminProfile);

        Profile tenantProfile = Profile.builder()
                .id(UUID.randomUUID())
                .email("tenant@stayfile.in")
                .role(UserRole.TENANT)
                .organization(organization)
                .build();
        tenantUser = new SecurityUser(tenantProfile);
    }

    @Test
    @DisplayName("Should successfully verify valid PAN without entity link (Ad-hoc check)")
    void testVerifyPan_Valid_Unlinked() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("ABCPV1234D")
                .name("John Doe")
                .build();

        CashfreePanResponse cfResponse = CashfreePanResponse.builder()
                .referenceId("103233")
                .pan("ABCPV1234D")
                .registeredName("JOHN DOE")
                .nameProvided("John Doe")
                .type("Individual")
                .valid(true)
                .message("PAN verified successfully")
                .nameMatchScore("100.00")
                .nameMatchResult("DIRECT_MATCH")
                .aadhaarSeedingStatus("Y")
                .panStatus("VALID")
                .panStatusCode("E")
                .panStatusDesc("EXISTING AND VALID")
                .build();

        when(cashfreeHttpClient.post(eq("/pan"), any(CashfreePanVerifyRequest.class), eq(CashfreePanResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        PanVerificationResponse response = kycVerificationService.verifyPan(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getPanLast4()).isEqualTo("234D");
        assertThat(response.getRegisteredName()).isEqualTo("JOHN DOE");
        assertThat(response.getNameMatchResult()).isEqualTo("DIRECT_MATCH");
        assertThat(response.getEntityType()).isEqualTo("Individual");
        assertThat(response.getAadhaarSeedingStatus()).isEqualTo("Y");
        assertThat(response.isLinkedToTenant()).isFalse();
        assertThat(response.isLinkedToLandlord()).isFalse();
    }

    @Test
    @DisplayName("Should verify PAN and link/update Tenant profile with VERIFIED status")
    void testVerifyPan_Valid_LinkedToTenant_DirectMatch() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("John Doe")
                .phone("+919876543210")
                .permanentAddress("Sector 62, Noida")
                .build();

        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("ABCPV1234D")
                .name("John Doe")
                .tenantId(tenantId)
                .build();

        CashfreePanResponse cfResponse = CashfreePanResponse.builder()
                .referenceId("103234")
                .pan("ABCPV1234D")
                .registeredName("JOHN DOE")
                .valid(true)
                .nameMatchScore("100.00")
                .nameMatchResult("DIRECT_MATCH")
                .type("Individual")
                .panStatus("VALID")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.post(eq("/pan"), any(CashfreePanVerifyRequest.class), eq(CashfreePanResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        PanVerificationResponse response = kycVerificationService.verifyPan(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.isLinkedToTenant()).isTrue();

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant updatedTenant = tenantCaptor.getValue();

        assertThat(updatedTenant.getKycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(updatedTenant.getIdProofLast4()).isEqualTo("XXXXXX234D");
        assertThat(updatedTenant.getIdProofNumber()).startsWith("ENC:GCM:v1:");
        assertThat(piiEncryptionService.decrypt(updatedTenant.getIdProofNumber())).isEqualTo("ABCPV1234D");
        assertThat(updatedTenant.getMetadata()).contains("pan_verification");
    }

    @Test
    @DisplayName("Should mark Tenant KYC status as REJECTED when PAN is invalid")
    void testVerifyPan_Invalid_RejectsTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Fake Person")
                .phone("+919876543210")
                .permanentAddress("Nowhere")
                .build();

        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("ABCDE0000Z")
                .tenantId(tenantId)
                .build();

        CashfreePanResponse cfResponse = CashfreePanResponse.builder()
                .referenceId("103235")
                .pan("ABCDE0000Z")
                .valid(false)
                .message("PAN is invalid")
                .panStatus("INVALID")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.post(eq("/pan"), any(CashfreePanVerifyRequest.class), eq(CashfreePanResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        PanVerificationResponse response = kycVerificationService.verifyPan(request, adminUser);

        assertThat(response.isValid()).isFalse();

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant updatedTenant = tenantCaptor.getValue();

        assertThat(updatedTenant.getKycStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("Should verify PAN and update Landlord encrypted PAN and metadata")
    void testVerifyPan_Valid_LinkedToLandlord() {
        UUID landlordId = UUID.randomUUID();
        Landlord landlord = Landlord.builder()
                .id(landlordId)
                .managingOrganization(organization)
                .legalName("Ramesh Kumar")
                .phone("+919988776655")
                .metadata("{}")
                .build();

        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("AAOCS4553K")
                .name("Ramesh Kumar")
                .landlordId(landlordId)
                .build();

        CashfreePanResponse cfResponse = CashfreePanResponse.builder()
                .referenceId("103236")
                .pan("AAOCS4553K")
                .registeredName("RAMESH KUMAR")
                .valid(true)
                .type("Individual")
                .panStatus("VALID")
                .build();

        when(landlordRepository.findByIdAndManagingOrganizationId(landlordId, orgId)).thenReturn(Optional.of(landlord));
        when(cashfreeHttpClient.post(eq("/pan"), any(CashfreePanVerifyRequest.class), eq(CashfreePanResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(landlordRepository.save(any(Landlord.class))).thenAnswer(i -> i.getArgument(0));

        PanVerificationResponse response = kycVerificationService.verifyPan(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.isLinkedToLandlord()).isTrue();

        ArgumentCaptor<Landlord> landlordCaptor = ArgumentCaptor.forClass(Landlord.class);
        verify(landlordRepository).save(landlordCaptor.capture());
        Landlord updatedLandlord = landlordCaptor.getValue();

        assertThat(updatedLandlord.getPanLast4()).isEqualTo("553K");
        assertThat(updatedLandlord.getPan()).startsWith("ENC:GCM:v1:");
        assertThat(piiEncryptionService.decrypt(updatedLandlord.getPan())).isEqualTo("AAOCS4553K");
        assertThat(updatedLandlord.getMetadata()).contains("pan_verification");
    }

    @Test
    @DisplayName("Tenant user should NOT be allowed to verify Landlord PAN")
    void testVerifyPan_TenantUser_CannotVerifyLandlord() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("AAOCS4553K")
                .landlordId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() -> kycVerificationService.verifyPan(request, tenantUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Tenants are not permitted to verify landlord KYC");
    }

    @Test
    @DisplayName("Should propagate IntegrationException when Cashfree API returns error")
    void testVerifyPan_CashfreeError_PropagatesIntegrationException() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .pan("ABCPV1234D")
                .build();

        when(cashfreeHttpClient.post(eq("/pan"), any(CashfreePanVerifyRequest.class), eq(CashfreePanResponse.class), eq(orgId)))
                .thenThrow(new IntegrationException("CASHFREE", 400, "{\"message\":\"Invalid format\"}", "Invalid PAN format"));

        assertThatThrownBy(() -> kycVerificationService.verifyPan(request, adminUser))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Invalid PAN format");
    }
}
