package com.bhartiyasaas.stayfile;

import com.bhartiyasaas.stayfile.dto.request.GstinVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.GstinVerificationResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeGstinResponse;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.impl.KybVerificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KybVerificationTests {
    @Mock private CashfreeHttpClient cashfreeHttpClient;
    @Mock private LandlordRepository landlordRepository;
    @Mock private com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository brandingSettingsRepository;
    @Mock private com.bhartiyasaas.stayfile.repository.OrganizationRepository organizationRepository;
    @Spy private PiiEncryptionService piiEncryptionService =
            new PiiEncryptionService("12345678901234567890123456789012");
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private KybVerificationServiceImpl service;

    private UUID organizationId;
    private SecurityUser adminUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        Organization organization = Organization.builder().id(organizationId).name("Test").build();
        Profile profile = Profile.builder().id(UUID.randomUUID()).email("admin@test.com")
                .role(UserRole.ADMIN).organization(organization).build();
        adminUser = new SecurityUser(profile);
    }

    @Test
    void verifyGstin_mapsCashfreeResponse() {
        GstinVerificationRequest request = new GstinVerificationRequest();
        request.setGstin("09ABCDE1234F1Z5");
        when(cashfreeHttpClient.post(eq("/gstin"), any(), eq(CashfreeGstinResponse.class), eq(organizationId)))
                .thenReturn(gstinResponse());

        GstinVerificationResponse response = service.verifyGstin(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getGstin()).isEqualTo("09ABCDE1234F1Z5");
        assertThat(response.getLegalName()).isEqualTo("ABC PRIVATE LIMITED");
    }

    private CashfreeGstinResponse gstinResponse() {
        CashfreeGstinResponse response = new CashfreeGstinResponse();
        response.setValid(true);
        response.setGstin("09ABCDE1234F1Z5");
        response.setLegalName("ABC PRIVATE LIMITED");
        response.setStatus("Active");
        response.setMessage("GSTIN Exists");
        return response;
    }
}
