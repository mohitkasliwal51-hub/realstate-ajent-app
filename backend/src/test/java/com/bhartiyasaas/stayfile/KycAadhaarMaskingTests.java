package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.response.AadhaarMaskingResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeAadhaarMaskingResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class KycAadhaarMaskingTests {

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
    @DisplayName("Should mask Aadhaar card image and update Tenant profile with masked URL")
    void testMaskAadhaar_Valid_LinkedToTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Sunil Kumar")
                .phone("+919123456789")
                .permanentAddress("Sector 14, Gurgaon")
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_front.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4, 5}
        );

        CashfreeAadhaarMaskingResponse cfResponse = CashfreeAadhaarMaskingResponse.builder()
                .status("VALID")
                .referenceId("354768")
                .verificationId("testverificationid")
                .imageLink("https://sandbox.cashfree.com/masked/download.jpg")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.postMultipart(eq("/aadhaar-masking"), any(MultiValueMap.class), any(), eq(CashfreeAadhaarMaskingResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        AadhaarMaskingResponse response = kycVerificationService.maskAadhaar(file, tenantId, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getStatus()).isEqualTo("VALID");
        assertThat(response.getReferenceId()).isEqualTo("354768");
        assertThat(response.isLinkedToTenant()).isTrue();

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant updatedTenant = tenantCaptor.getValue();

        assertThat(updatedTenant.getIdProofType()).isEqualTo("Aadhaar");
        assertThat(updatedTenant.getIdProofDocumentUrl()).isNotNull();
        assertThat(updatedTenant.getMetadata()).contains("aadhaar_masking");
    }

    @Test
    @DisplayName("Should return invalid response when Cashfree detects INVALID_DOCUMENT")
    void testMaskAadhaar_InvalidDocument() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .organization(organization)
                .fullName("Sunil Kumar")
                .phone("+919123456789")
                .permanentAddress("Sector 14, Gurgaon")
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blurry_photo.png",
                "image/png",
                new byte[]{1, 2, 3, 4, 5}
        );

        CashfreeAadhaarMaskingResponse cfResponse = CashfreeAadhaarMaskingResponse.builder()
                .status("INVALID_DOCUMENT")
                .referenceId("354769")
                .verificationId("testverificationid_invalid")
                .build();

        when(tenantRepository.findByIdAndOrganizationId(tenantId, orgId)).thenReturn(Optional.of(tenant));
        when(cashfreeHttpClient.postMultipart(eq("/aadhaar-masking"), any(MultiValueMap.class), any(), eq(CashfreeAadhaarMaskingResponse.class), eq(orgId)))
                .thenReturn(cfResponse);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        AadhaarMaskingResponse response = kycVerificationService.maskAadhaar(file, tenantId, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getStatus()).isEqualTo("INVALID_DOCUMENT");
        assertThat(response.getMessage()).contains("The uploaded file could not be recognized as a valid Aadhaar card");
    }

    @Test
    @DisplayName("Should reject invalid file format (non image/pdf)")
    void testMaskAadhaar_InvalidFileType_ThrowsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> kycVerificationService.maskAadhaar(file, null, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Please upload a valid Aadhaar file format");
    }

    @Test
    @DisplayName("Should reject empty file upload")
    void testMaskAadhaar_EmptyFile_ThrowsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "aadhaar.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() -> kycVerificationService.maskAadhaar(emptyFile, null, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Aadhaar card image or PDF file is required");
    }

    @Test
    @DisplayName("Should reject file exceeding 10 MB size limit")
    void testMaskAadhaar_FileSizeExceeds10MB_ThrowsBadRequest() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large_aadhaar.pdf",
                "application/pdf",
                new byte[1]
        ) {
            @Override
            public long getSize() {
                return 10L * 1024 * 1024 + 1; // 10MB + 1 byte
            }
        };

        assertThatThrownBy(() -> kycVerificationService.maskAadhaar(largeFile, null, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds the 10 MB limit");
    }

    @Test
    @DisplayName("Should successfully mask Aadhaar standalone without linking to a tenant")
    void testMaskAadhaar_StandaloneWithoutTenant() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_card.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        CashfreeAadhaarMaskingResponse cfResponse = CashfreeAadhaarMaskingResponse.builder()
                .status("VALID")
                .referenceId("987654")
                .verificationId("test_standalone_id")
                .imageLink("https://sandbox.cashfree.com/masked/doc.png")
                .build();

        when(cashfreeHttpClient.postMultipart(eq("/aadhaar-masking"), any(MultiValueMap.class), any(), eq(CashfreeAadhaarMaskingResponse.class), eq(orgId)))
                .thenReturn(cfResponse);

        AadhaarMaskingResponse response = kycVerificationService.maskAadhaar(file, null, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getStatus()).isEqualTo("VALID");
        assertThat(response.isLinkedToTenant()).isFalse();
        assertThat(response.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when tenant ID does not belong to organization")
    void testMaskAadhaar_TenantNotFoundInOrg_ThrowsException() {
        UUID unknownTenantId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_card.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        when(tenantRepository.findByIdAndOrganizationId(unknownTenantId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycVerificationService.maskAadhaar(file, unknownTenantId, adminUser))
                .isInstanceOf(com.bhartiyasaas.stayfile.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Tenant not found with ID");
    }
}
