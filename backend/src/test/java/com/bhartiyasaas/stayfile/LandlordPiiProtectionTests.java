package com.bhartiyasaas.stayfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bhartiyasaas.stayfile.dto.request.LandlordRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.mapper.LandlordMapper;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.security.OrganizationPolicyService;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.impl.LandlordServiceImpl;

@ExtendWith(MockitoExtension.class)
class LandlordPiiProtectionTests {

    @Mock
    private LandlordRepository landlordRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private LandlordMapper landlordMapper;
    @Mock
    private OrganizationPolicyService organizationPolicyService;

    // Use actual PiiEncryptionService with a 256-bit key for realistic crypto tests
    @Spy
    private PiiEncryptionService piiEncryptionService = new PiiEncryptionService("12345678901234567890123456789012");

    @InjectMocks
    private LandlordServiceImpl landlordService;

    private UUID orgId;
    private Organization organization;
    private SecurityUser adminUser;
    private SecurityUser agentUser;

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

        com.bhartiyasaas.stayfile.entity.Profile agentProfile = com.bhartiyasaas.stayfile.entity.Profile.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("agent@test.com")
                .passwordHash("pass")
                .role(UserRole.AGENT)
                .isActive(true)
                .build();
        agentUser = new SecurityUser(agentProfile);
    }

    @Test
    void createLandlord_encryptsPanAndBankAccountAndSetsLast4() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(landlordMapper.toEntity(any())).thenReturn(new Landlord());
        when(landlordRepository.save(any(Landlord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landlordMapper.toResponse(any(Landlord.class))).thenAnswer(invocation -> {
            Landlord l = invocation.getArgument(0);
            return LandlordResponse.builder()
                    .pan(l.getPan())
                    .bankAccountNumber(l.getBankAccountNumber())
                    .build();
        });

        LandlordRequest request = LandlordRequest.builder()
                .legalName("Ramesh Gupta")
                .phone("+919876543210")
                .pan("ABCDE1234F")
                .bankAccountNumber("123456789012")
                .build();

        LandlordResponse response = landlordService.createLandlord(request, adminUser);

        // Admin receives decrypted values
        assertThat(response.getPan()).isEqualTo("ABCDE1234F");
        assertThat(response.getPanLast4()).isEqualTo("234F");
        assertThat(response.getBankAccountNumber()).isEqualTo("123456789012");
        assertThat(response.getBankAccountLast4()).isEqualTo("9012");
    }

    @Test
    void getLandlordById_masksPanAndBankAccountForAgentRole() {
        String encryptedPan = piiEncryptionService.encrypt("ABCDE1234F");
        String encryptedBank = piiEncryptionService.encrypt("123456789012");

        Landlord landlord = Landlord.builder()
                .id(UUID.randomUUID())
                .managingOrganization(organization)
                .legalName("Ramesh Gupta")
                .pan(encryptedPan)
                .panLast4("234F")
                .bankAccountNumber(encryptedBank)
                .bankAccountLast4("9012")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(landlordRepository.findByIdAndManagingOrganizationId(landlord.getId(), orgId)).thenReturn(Optional.of(landlord));
        when(landlordMapper.toResponse(landlord)).thenReturn(LandlordResponse.builder().build());

        LandlordResponse response = landlordService.getLandlordById(landlord.getId(), agentUser);

        // For agent, PAN must be masked and bank account masked
        assertThat(response.getPan()).isEqualTo("XXXXXX234F");
        assertThat(response.getBankAccountNumber()).isEqualTo("XXXX-XXXX-9012");
        assertThat(response.getPanLast4()).isEqualTo("234F");
        assertThat(response.getBankAccountLast4()).isEqualTo("9012");
    }
}
