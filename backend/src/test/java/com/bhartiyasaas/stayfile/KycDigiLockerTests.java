package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.DigiLockerInitiateRequest;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerStatusResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerVerificationCompleteResponse;
import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerAadhaarDocumentResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerCreateUrlRequest;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerCreateUrlResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerStatusResponse;
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
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class KycDigiLockerTests {

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
    }

    @Test
    @DisplayName("Should successfully initiate DigiLocker session and return consent URL")
    void testInitiateDigiLocker_Success() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Amit Sharma")
                .phone("+919876543210")
                .permanentAddress("Sector 15, Noida")
                .build();

        DigiLockerInitiateRequest request = DigiLockerInitiateRequest.builder()
                .tenantId(tenantId)
                .redirectUrl("https://stayfile.in/kyc/callback")
                .build();

        CashfreeDigiLockerCreateUrlResponse cfResponse = CashfreeDigiLockerCreateUrlResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .url("https://verification-test.cashfree.com/dgl/h7562ci7us0")
                .status("PENDING")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.post(eq("/digilocker"), any(CashfreeDigiLockerCreateUrlRequest.class), eq(CashfreeDigiLockerCreateUrlResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        DigiLockerInitiateResponse response = kycVerificationService.initiateDigiLocker(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getConsentUrl()).isEqualTo("https://verification-test.cashfree.com/dgl/h7562ci7us0");
        assertThat(response.getVerificationId()).startsWith("sf_dgl_");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getExpiresInMinutes()).isEqualTo(10);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getMetadata()).contains("digilocker_session");
    }

    @Test
    @DisplayName("Should retrieve DigiLocker session status when AUTHENTICATED")
    void testGetDigiLockerStatus_Authenticated() {
        CashfreeDigiLockerStatusResponse cfResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .status("AUTHENTICATED")
                .build();

        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        DigiLockerStatusResponse response = kycVerificationService.getDigiLockerStatus("sf_dgl_123456", adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("AUTHENTICATED");
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.isCanComplete()).isTrue();
        assertThat(response.getActionRequired()).isEqualTo("PROCEED_TO_COMPLETE");
    }

    @Test
    @DisplayName("Should retrieve DigiLocker session status when PENDING")
    void testGetDigiLockerStatus_Pending() {
        CashfreeDigiLockerStatusResponse cfResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .status("PENDING")
                .build();

        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        DigiLockerStatusResponse response = kycVerificationService.getDigiLockerStatus("sf_dgl_123456", adminUser);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.isAuthenticated()).isFalse();
        assertThat(response.isCanComplete()).isFalse();
        assertThat(response.getActionRequired()).isEqualTo("WAIT_FOR_USER_OTP");
        assertThat(response.getMessage()).contains("Waiting for user to complete login");
    }

    @Test
    @DisplayName("Should retrieve DigiLocker session status when EXPIRED")
    void testGetDigiLockerStatus_Expired() {
        CashfreeDigiLockerStatusResponse cfResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .status("EXPIRED")
                .build();

        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        DigiLockerStatusResponse response = kycVerificationService.getDigiLockerStatus("sf_dgl_123456", adminUser);

        assertThat(response.getStatus()).isEqualTo("EXPIRED");
        assertThat(response.isExpired()).isTrue();
        assertThat(response.isCanComplete()).isFalse();
        assertThat(response.getActionRequired()).isEqualTo("REINITIATE_SESSION");
    }

    @Test
    @DisplayName("Should retrieve DigiLocker session status when CONSENT_DENIED")
    void testGetDigiLockerStatus_ConsentDenied() {
        CashfreeDigiLockerStatusResponse cfResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .status("CONSENT_DENIED")
                .build();

        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        DigiLockerStatusResponse response = kycVerificationService.getDigiLockerStatus("sf_dgl_123456", adminUser);

        assertThat(response.getStatus()).isEqualTo("CONSENT_DENIED");
        assertThat(response.isConsentDenied()).isTrue();
        assertThat(response.isCanComplete()).isFalse();
        assertThat(response.getActionRequired()).isEqualTo("REINITIATE_SESSION");
    }

    @Test
    @DisplayName("Should complete DigiLocker verification, fetch verified UIDAI Aadhaar, and update Tenant")
    void testCompleteDigiLocker_Success_UpdatesTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Amit Sharma")
                .phone("+919876543210")
                .permanentAddress("Sector 15, Noida")
                .build();

        CashfreeDigiLockerStatusResponse statusResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .referenceId("98765")
                .status("AUTHENTICATED")
                .build();

        CashfreeDigiLockerAadhaarDocumentResponse cfResponse = CashfreeDigiLockerAadhaarDocumentResponse.builder()
                .status("VALID")
                .referenceId("98765")
                .verificationId("sf_dgl_123456")
                .documentType("AADHAAR")
                .name("AMIT SHARMA")
                .uid("XXXXXXXX4321")
                .careOf("S/O RAMESH SHARMA")
                .dob("12-05-1990")
                .gender("M")
                .splitAddress(Map.of(
                        "house", "Flat 301",
                        "street", "MG Road",
                        "district", "Noida",
                        "state", "Uttar Pradesh",
                        "pincode", "201301"
                ))
                .image("dGVzdC1waG90by1ieXRlcw==") // Base64
                .build();

        FileUploadResponse fileResponse = FileUploadResponse.builder()
                .url("/api/v1/files/private/tenant-kyc/uidai_photo.jpg")
                .key("tenant-kyc/uidai_photo.jpg")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(statusResponse);
        when(cashfreeHttpClient.get(eq("/digilocker/document/AADHAAR"), any(), eq(CashfreeDigiLockerAadhaarDocumentResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(storageService.storeFile(any(), eq(FileCategory.TENANT_KYC), eq(orgId)))
                .thenReturn(fileResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        DigiLockerVerificationCompleteResponse response = kycVerificationService.completeDigiLocker("sf_dgl_123456", tenantId, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.isVerified()).isTrue();
        assertThat(response.getLegalName()).isEqualTo("AMIT SHARMA");
        assertThat(response.getMaskedAadhaar()).isEqualTo("XXXXXXXX4321");
        assertThat(response.getDob()).isEqualTo("12-05-1990");
        assertThat(response.getGender()).isEqualTo("M");
        assertThat(response.getCareOf()).isEqualTo("S/O RAMESH SHARMA");
        assertThat(response.getPermanentAddress()).contains("Flat 301, MG Road, Noida, Uttar Pradesh - 201301");
        assertThat(response.getPhotoDocumentUrl()).isEqualTo("/api/v1/files/private/tenant-kyc/uidai_photo.jpg");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant updatedTenant = tenantCaptor.getValue();

        assertThat(updatedTenant.getKycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(updatedTenant.getFullName()).isEqualTo("AMIT SHARMA");
        assertThat(updatedTenant.getIdProofType()).isEqualTo("Aadhaar");
        assertThat(updatedTenant.getIdProofLast4()).isEqualTo("XXXX-XXXX-4321");
        assertThat(updatedTenant.getPermanentAddress()).contains("Flat 301, MG Road, Noida, Uttar Pradesh - 201301");
        assertThat(updatedTenant.getIdProofDocumentUrl()).isEqualTo("/api/v1/files/private/tenant-kyc/uidai_photo.jpg");
        assertThat(updatedTenant.getMetadata()).contains("digilocker_verification");
    }

    @Test
    @DisplayName("Should reject premature completion when DigiLocker session is still PENDING")
    void testCompleteDigiLocker_PrematurePending_DoesNotFetchDocument() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Amit Sharma")
                .build();

        CashfreeDigiLockerStatusResponse statusResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .status("PENDING")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(statusResponse);

        DigiLockerVerificationCompleteResponse response = kycVerificationService.completeDigiLocker("sf_dgl_123456", tenantId, adminUser);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getMessage()).contains("DigiLocker session is still pending");

        // Verify document was NOT fetched prematurely
        verify(cashfreeHttpClient, never()).get(eq("/digilocker/document/AADHAAR"), any(), any(), any());
    }

    @Test
    @DisplayName("Should mark Tenant as REJECTED when user denied consent on DigiLocker")
    void testCompleteDigiLocker_ConsentDenied_RejectsTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Amit Sharma")
                .build();

        CashfreeDigiLockerStatusResponse statusResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .status("CONSENT_DENIED")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(statusResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        DigiLockerVerificationCompleteResponse response = kycVerificationService.completeDigiLocker("sf_dgl_123456", tenantId, adminUser);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getStatus()).isEqualTo("CONSENT_DENIED");
        assertThat(response.getMessage()).contains("denied document consent");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getKycStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("Should mark Tenant as REJECTED when Aadhaar is not linked in DigiLocker (AADHAAR_NOT_LINKED)")
    void testCompleteDigiLocker_AadhaarNotLinked_RejectsTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Sunil Verma")
                .build();

        CashfreeDigiLockerStatusResponse statusResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .status("AUTHENTICATED")
                .build();

        CashfreeDigiLockerAadhaarDocumentResponse docResponse = CashfreeDigiLockerAadhaarDocumentResponse.builder()
                .status("AADHAAR_NOT_LINKED")
                .referenceId("98765")
                .verificationId("sf_dgl_123456")
                .documentType("AADHAAR")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(statusResponse);
        when(cashfreeHttpClient.get(eq("/digilocker/document/AADHAAR"), any(), eq(CashfreeDigiLockerAadhaarDocumentResponse.class), eq(orgId)))
                .thenReturn(docResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        DigiLockerVerificationCompleteResponse response = kycVerificationService.completeDigiLocker("sf_dgl_123456", tenantId, adminUser);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getStatus()).isEqualTo("AADHAAR_NOT_LINKED");
        assertThat(response.getMessage()).contains("Aadhaar is not linked to their DigiLocker account");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getKycStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("Should keep DigiLocker completion pending when document validation returns 202")
    void testCompleteDigiLocker_ValidationPending_DoesNotRejectTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Amit Sharma")
                .build();

        CashfreeDigiLockerStatusResponse statusResponse = CashfreeDigiLockerStatusResponse.builder()
                .verificationId("sf_dgl_123456")
                .status("AUTHENTICATED")
                .build();
        CashfreeDigiLockerAadhaarDocumentResponse docResponse =
                CashfreeDigiLockerAadhaarDocumentResponse.builder()
                        .code("VALIDATION_PENDING")
                        .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.get(eq("/digilocker"), any(), eq(CashfreeDigiLockerStatusResponse.class), eq(orgId)))
                .thenReturn(statusResponse);
        when(cashfreeHttpClient.get(eq("/digilocker/document/AADHAAR"), any(), eq(CashfreeDigiLockerAadhaarDocumentResponse.class), eq(orgId)))
                .thenReturn(docResponse);

        DigiLockerVerificationCompleteResponse response =
                kycVerificationService.completeDigiLocker("sf_dgl_123456", tenantId, adminUser);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_PENDING");
        assertThat(response.getMessage()).contains("still pending");
        verify(tenantRepository, never()).save(any(Tenant.class));
    }
}
