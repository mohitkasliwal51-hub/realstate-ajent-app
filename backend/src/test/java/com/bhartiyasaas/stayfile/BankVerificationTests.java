package com.bhartiyasaas.stayfile;

import com.bhartiyasaas.stayfile.dto.request.BankIfscLookupRequest;
import com.bhartiyasaas.stayfile.dto.request.BankRpdInitiateRequest;
import com.bhartiyasaas.stayfile.dto.request.BankSyncVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.BankUpiVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.BankIfscLookupResponse;
import com.bhartiyasaas.stayfile.dto.response.BankRpdInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.BankVerificationResultResponse;
import com.bhartiyasaas.stayfile.entity.BrandingSettings;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.*;
import com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.impl.BankVerificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankVerificationTests {

    @Mock private CashfreeHttpClient cashfreeHttpClient;
    @Mock private LandlordRepository landlordRepository;
    @Mock private BrandingSettingsRepository brandingSettingsRepository;
    @Mock private OrganizationRepository organizationRepository;

    @Spy private PiiEncryptionService piiEncryptionService =
            new PiiEncryptionService("12345678901234567890123456789012");
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private BankVerificationServiceImpl service;

    private UUID organizationId;
    private Organization organization;
    private SecurityUser adminUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = Organization.builder().id(organizationId).name("StayFile Realty").build();
        Profile profile = Profile.builder()
                .id(UUID.randomUUID())
                .email("admin@stayfile.in")
                .role(UserRole.ADMIN)
                .organization(organization)
                .build();
        adminUser = new SecurityUser(profile);
    }

    @Test
    @DisplayName("Reverse Penny Drop - Initiate returns dynamic QR code and UPI links")
    void initiateReversePennyDrop_returnsQrAndLinks() {
        BankRpdInitiateRequest request = new BankRpdInitiateRequest();
        request.setPhone("9876543210");
        request.setName("Rahul Sharma");

        CashfreeRpdCreateResponse cfResponse = new CashfreeRpdCreateResponse();
        cfResponse.setVerificationId("SF-RPD-12345");
        cfResponse.setRefId("CF-REF-9988");
        cfResponse.setStatus("PENDING");
        cfResponse.setQrCode("data:image/png;base64,iVBORw0KGgo...");
        cfResponse.setUpiLink("upi://pay?pa=cashfree.rpd@icici&am=1.00");
        cfResponse.setGpay("tez://upi/pay?...");
        cfResponse.setPhonepe("phonepe://pay?...");

        when(cashfreeHttpClient.post(eq("/reverse-penny-drop"), any(), eq(CashfreeRpdCreateResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);

        BankRpdInitiateResponse response = service.initiateReversePennyDrop(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getVerificationId()).isEqualTo("SF-RPD-12345");
        assertThat(response.getQrCode()).contains("base64");
        assertThat(response.getUpiIntentUrl()).contains("upi://pay");
        assertThat(response.getPspLinks()).containsKey("gpay");
    }

    @Test
    @DisplayName("Reverse Penny Drop - Check status succeeds and persists verified account to BrandingSettings")
    void checkReversePennyDropStatus_persistsToBrandingSettings() {
        CashfreeRpdStatusResponse cfResponse = new CashfreeRpdStatusResponse();
        cfResponse.setVerificationId("SF-RPD-12345");
        cfResponse.setStatus("SUCCESS");
        cfResponse.setBankAccount("123456789012");
        cfResponse.setIfsc("HDFC0001234");
        cfResponse.setNameAtBank("RAHUL SHARMA");
        cfResponse.setUpiId("rahul@okhdfcbank");

        when(cashfreeHttpClient.get(eq("/remitter/status"), any(), eq(CashfreeRpdStatusResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);
        when(brandingSettingsRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(BrandingSettings.builder().organization(organization).legalBusinessName("StayFile Realty").build()));

        BankVerificationResultResponse response = service.checkReversePennyDropStatus("SF-RPD-12345", null, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getVerificationMode()).isEqualTo("REVERSE_PENNY_DROP");
        assertThat(response.getAccountNumber()).endsWith("9012");
        assertThat(response.getIfsc()).isEqualTo("HDFC0001234");
        assertThat(response.getAccountHolderName()).isEqualTo("RAHUL SHARMA");
        assertThat(response.getUpiId()).isEqualTo("rahul@okhdfcbank");

        verify(brandingSettingsRepository).save(any(BrandingSettings.class));
    }

    @Test
    @DisplayName("IFSC Lookup - Returns branch, city, and rail details")
    void lookupIfsc_returnsBranchDetails() {
        BankIfscLookupRequest request = new BankIfscLookupRequest();
        request.setIfsc("HDFC0000001");

        CashfreeIfscResponse cfResponse = new CashfreeIfscResponse();
        cfResponse.setStatus("VALID");
        cfResponse.setBank("HDFC BANK");
        cfResponse.setIfsc("HDFC0000001");
        cfResponse.setBranch("MUMBAI NARIMAN POINT");
        cfResponse.setCity("MUMBAI");
        cfResponse.setState("MAHARASHTRA");
        cfResponse.setMicr("400240002");
        cfResponse.setImps("Live");
        cfResponse.setRtgs("Live");

        when(cashfreeHttpClient.post(eq("/ifsc"), any(), eq(CashfreeIfscResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);

        BankIfscLookupResponse response = service.lookupIfsc(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getBank()).isEqualTo("HDFC BANK");
        assertThat(response.getBranch()).isEqualTo("MUMBAI NARIMAN POINT");
        assertThat(response.getCity()).isEqualTo("MUMBAI");
        assertThat(response.getImps()).isEqualTo("Live");
    }

    @Test
    @DisplayName("BAV V2 Sync - Penny drop verifies account and returns fuzzy name match score")
    void verifyBankAccountSync_valid_persistsToBranding() {
        BankSyncVerificationRequest request = new BankSyncVerificationRequest();
        request.setBankAccount("026291800001191");
        request.setIfsc("YESB0000001");
        request.setAccountHolderName("Rahul Sharma");

        CashfreeBavSyncResponse cfResponse = new CashfreeBavSyncResponse();
        cfResponse.setAccountStatus("VALID");
        cfResponse.setNameAtBank("RAHUL SHARMA");
        cfResponse.setBankName("YES BANK");
        cfResponse.setBranch("MUMBAI MAIN");
        cfResponse.setNameMatchScore("100.00");
        cfResponse.setNameMatchResult("EXACT_MATCH");
        cfResponse.setUtr("404223241811");

        when(cashfreeHttpClient.post(eq("/bank-account/sync"), any(), eq(CashfreeBavSyncResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);
        when(brandingSettingsRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(BrandingSettings.builder().organization(organization).legalBusinessName("StayFile Realty").build()));

        BankVerificationResultResponse response = service.verifyBankAccountSync(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getVerificationMode()).isEqualTo("BAV_SYNC");
        assertThat(response.getAccountHolderName()).isEqualTo("RAHUL SHARMA");
        assertThat(response.getBankName()).isEqualTo("YES BANK");
        assertThat(response.getNameMatchScore()).isEqualTo("100.00");
        assertThat(response.getUtr()).isEqualTo("404223241811");

        verify(brandingSettingsRepository).save(any(BrandingSettings.class));
    }

    @Test
    @DisplayName("BAV V2 Sync - Persists verified account to Landlord entity when landlordId is present")
    void verifyBankAccountSync_valid_persistsToLandlord() {
        UUID landlordId = UUID.randomUUID();
        Landlord landlord = Landlord.builder()
                .id(landlordId)
                .managingOrganization(organization)
                .legalName("Sunil Patel")
                .phone("9876543210")
                .metadata("{}")
                .build();

        BankSyncVerificationRequest request = new BankSyncVerificationRequest();
        request.setLandlordId(landlordId);
        request.setBankAccount("50100234567890");
        request.setIfsc("HDFC0001234");
        request.setAccountHolderName("Sunil Patel");

        CashfreeBavSyncResponse cfResponse = new CashfreeBavSyncResponse();
        cfResponse.setAccountStatus("VALID");
        cfResponse.setNameAtBank("SUNIL PATEL");
        cfResponse.setBankName("HDFC BANK");
        cfResponse.setBranch("KORAMANGALA");
        cfResponse.setNameMatchScore("100.00");
        cfResponse.setUtr("UTR987654321");

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(cashfreeHttpClient.post(eq("/bank-account/sync"), any(), eq(CashfreeBavSyncResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);

        BankVerificationResultResponse response = service.verifyBankAccountSync(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.isLinkedToLandlord()).isTrue();
        assertThat(response.getAccountHolderName()).isEqualTo("SUNIL PATEL");

        verify(landlordRepository).save(any(Landlord.class));
        assertThat(landlord.getBankIfscCode()).isEqualTo("HDFC0001234");
        assertThat(landlord.getAccountHolderName()).isEqualTo("SUNIL PATEL");
        assertThat(landlord.getBankAccountLast4()).isEqualTo("7890");
    }

    @Test
    @DisplayName("UPI Penny Drop - Validates VPA and saves VPA + Name")
    void verifyUpiPennyDrop_valid() {
        BankUpiVerificationRequest request = new BankUpiVerificationRequest();
        request.setVpa("stayfile@icici");
        request.setName("StayFile Realty");

        CashfreeUpiPennyDropResponse cfResponse = new CashfreeUpiPennyDropResponse();
        cfResponse.setStatus("SUCCESS");
        cfResponse.setVpa("stayfile@icici");
        cfResponse.setNameAtBank("STAYFILE REALTY PVT LTD");
        cfResponse.setNameMatchScore(95);
        cfResponse.setNameMatchResult("GOOD_PARTIAL_MATCH");

        when(cashfreeHttpClient.post(eq("/upi/penny-drop"), any(), eq(CashfreeUpiPennyDropResponse.class), eq(organizationId)))
                .thenReturn(cfResponse);
        when(brandingSettingsRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(BrandingSettings.builder().organization(organization).legalBusinessName("StayFile Realty").build()));

        BankVerificationResultResponse response = service.verifyUpiPennyDrop(request, adminUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getVerificationMode()).isEqualTo("UPI_PENNY_DROP");
        assertThat(response.getUpiId()).isEqualTo("stayfile@icici");
        assertThat(response.getAccountHolderName()).isEqualTo("STAYFILE REALTY PVT LTD");
        assertThat(response.getNameMatchScore()).isEqualTo("95");

        verify(brandingSettingsRepository).save(any(BrandingSettings.class));
    }
}
