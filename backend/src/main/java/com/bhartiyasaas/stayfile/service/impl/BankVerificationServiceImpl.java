package com.bhartiyasaas.stayfile.service.impl;

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
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.*;
import com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.BankVerificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankVerificationServiceImpl implements BankVerificationService {

    private final CashfreeHttpClient cashfreeHttpClient;
    private final LandlordRepository landlordRepository;
    private final BrandingSettingsRepository brandingSettingsRepository;
    private final OrganizationRepository organizationRepository;
    private final PiiEncryptionService piiEncryptionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public BankRpdInitiateResponse initiateReversePennyDrop(BankRpdInitiateRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);

        String verificationId = "SF-RPD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        String name = request.getName();

        if (landlord != null) {
            if (name == null || name.isBlank()) name = landlord.getLegalName();
        }

        if (name == null || name.isBlank()) {
            name = currentUser.getUsername();
        }
        name = trimToNull(name);

        String redirectUrl = trimToNull(request.getRedirectUrl());
        if (redirectUrl != null && !isHttpsUrl(redirectUrl)) {
            throw new BadRequestException("Reverse penny drop redirect URL must be a valid HTTPS URL.");
        }

        CashfreeRpdCreateRequest cfRequest = CashfreeRpdCreateRequest.builder()
                .verificationId(verificationId)
                .name(name)
                .redirectUrl(redirectUrl)
                .build();

        log.info("Initiating Cashfree Reverse Penny Drop with verificationId: {} for org: {}", verificationId, organizationId);
        CashfreeRpdCreateResponse cfResponse = cashfreeHttpClient.post(
                "/reverse-penny-drop",
                cfRequest,
                CashfreeRpdCreateResponse.class,
                organizationId
        );

        return BankRpdInitiateResponse.builder()
                .verificationId(cfResponse.getVerificationId() != null ? cfResponse.getVerificationId() : verificationId)
                .refId(cfResponse.getRefIdAsString())
                .status(cfResponse.getStatus())
                .message(cfResponse.getMessage() != null ? cfResponse.getMessage() : "Reverse penny drop initiated")
                .upiIntentUrl(cfResponse.getUpiLink())
                .qrCode(cfResponse.getQrCode())
                .pspLinks(cfResponse.getPspLinks())
                .validUpto(cfResponse.getValidUpto())
                .url(cfResponse.getUrl())
                .build();
    }

    @Override
    @Transactional
    public BankVerificationResultResponse checkReversePennyDropStatus(String verificationId, UUID landlordId, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(landlordId, organizationId, currentUser);

        log.info("Checking Cashfree RPD status for verificationId: {} in org: {}", verificationId, organizationId);
        CashfreeRpdStatusResponse cfResponse = cashfreeHttpClient.get(
                "/remitter/status",
                Map.of("verification_id", verificationId),
                CashfreeRpdStatusResponse.class,
                organizationId
        );

        boolean valid = cfResponse != null && cfResponse.isSuccess();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("verification_mode", "REVERSE_PENNY_DROP");
        metadata.put("verification_id", verificationId);
        metadata.put("ref_id", cfResponse != null ? cfResponse.getRefIdAsString() : null);
        metadata.put("status", cfResponse != null ? cfResponse.getStatus() : "UNKNOWN");
        metadata.put("amount", cfResponse != null ? cfResponse.getAmount() : "1.00");
        metadata.put("refund_status", cfResponse != null ? cfResponse.getRefundStatus() : null);
        metadata.put("verified_at", OffsetDateTime.now().toString());

        if (valid) {
            persistBankDetails(
                    organizationId,
                    landlord,
                    cfResponse.getBankAccount(),
                    cfResponse.getIfsc(),
                    null, // bankName resolved from IFSC or set at bank
                    cfResponse.getNameAtBank(),
                    cfResponse.getUpiId(),
                    "rpd_verification",
                    metadata
            );
        }

        return BankVerificationResultResponse.builder()
                .valid(valid)
                .verificationMode("REVERSE_PENNY_DROP")
                .accountNumber(maskAccountNumber(cfResponse != null ? cfResponse.getBankAccount() : null))
                .ifsc(cfResponse != null ? cfResponse.getIfsc() : null)
                .accountHolderName(cfResponse != null ? cfResponse.getNameAtBank() : null)
                .upiId(cfResponse != null ? cfResponse.getUpiId() : null)
                .referenceId(cfResponse != null ? cfResponse.getRefIdAsString() : null)
                .message(valid ? "Bank account verified successfully via Reverse Penny Drop" : "Verification in progress or pending user payment")
                .linkedToLandlord(landlord != null)
                .metadata(metadata)
                .build();
    }

    @Override
    public BankIfscLookupResponse lookupIfsc(BankIfscLookupRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        String ifsc = request.getIfsc().trim().toUpperCase();

        log.info("Looking up IFSC: {} for org: {}", ifsc, organizationId);
        CashfreeIfscResponse cfResponse = cashfreeHttpClient.post(
                "/ifsc",
                CashfreeIfscRequest.builder()
                        .verificationId("SF-IFSC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                        .ifsc(ifsc)
                        .build(),
                CashfreeIfscResponse.class,
                organizationId
        );

        boolean valid = cfResponse != null && cfResponse.isValid();

        return BankIfscLookupResponse.builder()
                .valid(valid)
                .ifsc(ifsc)
                .bank(cfResponse != null ? cfResponse.getBank() : null)
                .branch(cfResponse != null ? cfResponse.getBranch() : null)
                .address(cfResponse != null ? cfResponse.getAddress() : null)
                .city(cfResponse != null ? cfResponse.getCity() : null)
                .state(cfResponse != null ? cfResponse.getState() : null)
                .micr(cfResponse != null ? cfResponse.getMicr() : null)
                .imps(cfResponse != null ? cfResponse.getImps() : null)
                .rtgs(cfResponse != null ? cfResponse.getRtgs() : null)
                .neft(cfResponse != null ? cfResponse.getNeft() : null)
                .upi(cfResponse != null ? cfResponse.getUpi() : null)
                .message(valid ? "IFSC details fetched successfully" : "Invalid IFSC code")
                .build();
    }

    @Override
    @Transactional
    public BankVerificationResultResponse verifyBankAccountSync(BankSyncVerificationRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);

        String rawAccount = request.getBankAccount().trim();
        String ifsc = request.getIfsc().trim().toUpperCase();
        String accountHolderName = trimToNull(request.getAccountHolderName());

        CashfreeBavSyncRequest cfRequest = CashfreeBavSyncRequest.builder()
                .bankAccount(rawAccount)
                .ifsc(ifsc)
                .name(accountHolderName)
                .phone(trimToNull(request.getPhone()))
                .build();

        log.info("Calling Cashfree BAV V2 Sync for bank account: ****{} IFSC: {} in org: {}",
                rawAccount.length() > 4 ? rawAccount.substring(rawAccount.length() - 4) : rawAccount, ifsc, organizationId);

        CashfreeBavSyncResponse cfResponse = cashfreeHttpClient.post(
                "/bank-account/sync",
                cfRequest,
                CashfreeBavSyncResponse.class,
                organizationId
        );

        boolean valid = cfResponse != null && cfResponse.isValid();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("verification_mode", "BAV_SYNC");
        metadata.put("reference_id", cfResponse != null ? cfResponse.getReferenceIdAsString() : null);
        metadata.put("account_status", cfResponse != null ? cfResponse.getAccountStatus() : null);
        metadata.put("account_status_code", cfResponse != null ? cfResponse.getAccountStatusCode() : null);
        metadata.put("name_at_bank", cfResponse != null ? cfResponse.getNameAtBank() : null);
        metadata.put("bank_name", cfResponse != null ? cfResponse.getBankName() : null);
        metadata.put("branch", cfResponse != null ? cfResponse.getBranch() : null);
        metadata.put("name_match_score", cfResponse != null ? cfResponse.getNameMatchScore() : null);
        metadata.put("name_match_result", cfResponse != null ? cfResponse.getNameMatchResult() : null);
        metadata.put("utr", cfResponse != null ? cfResponse.getUtr() : null);
        metadata.put("verified_at", OffsetDateTime.now().toString());

        if (valid) {
            persistBankDetails(
                    organizationId,
                    landlord,
                    rawAccount,
                    ifsc,
                    cfResponse.getBankName(),
                    cfResponse.getNameAtBank() != null ? cfResponse.getNameAtBank() : accountHolderName,
                    null,
                    "bav_verification",
                    metadata
            );
        }

        return BankVerificationResultResponse.builder()
                .valid(valid)
                .verificationMode("BAV_SYNC")
                .accountNumber(maskAccountNumber(rawAccount))
                .ifsc(ifsc)
                .bankName(cfResponse != null ? cfResponse.getBankName() : null)
                .branch(cfResponse != null ? cfResponse.getBranch() : null)
                .accountHolderName(cfResponse != null ? cfResponse.getNameAtBank() : accountHolderName)
                .nameMatchScore(cfResponse != null ? cfResponse.getNameMatchScore() : null)
                .nameMatchResult(cfResponse != null ? cfResponse.getNameMatchResult() : null)
                .utr(cfResponse != null ? cfResponse.getUtr() : null)
                .referenceId(cfResponse != null ? cfResponse.getReferenceIdAsString() : null)
                .message(cfResponse != null && cfResponse.getMessage() != null ? cfResponse.getMessage() : (valid ? "Bank account verified successfully" : "Bank account verification failed"))
                .linkedToLandlord(landlord != null)
                .metadata(metadata)
                .build();
    }

    @Override
    @Transactional
    public BankVerificationResultResponse verifyUpiPennyDrop(BankUpiVerificationRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);

        String vpa = request.getVpa().trim().toLowerCase();
        String name = trimToNull(request.getName());

        CashfreeUpiPennyDropRequest cfRequest = CashfreeUpiPennyDropRequest.builder()
                .verificationId("SF-UPI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14))
                .vpa(vpa)
                .name(name)
                .userConsent(CashfreeUpiPennyDropRequest.UserConsent.builder()
                        .obtained(true)
                        .type("EXPLICIT")
                        .timestamp(OffsetDateTime.now().toString())
                        .purpose("Bank account verification via UPI penny drop")
                        .build())
                .build();

        log.info("Calling Cashfree UPI Penny Drop for VPA: {} in org: {}", vpa, organizationId);
        CashfreeUpiPennyDropResponse cfResponse = cashfreeHttpClient.post(
                "/upi/penny-drop",
                cfRequest,
                CashfreeUpiPennyDropResponse.class,
                organizationId
        );

        boolean valid = cfResponse != null && cfResponse.isSuccess();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("verification_mode", "UPI_PENNY_DROP");
        metadata.put("reference_id", cfResponse != null ? cfResponse.getRefIdAsString() : null);
        metadata.put("status", cfResponse != null ? cfResponse.getStatus() : null);
        metadata.put("vpa", vpa);
        metadata.put("name_at_bank", cfResponse != null ? cfResponse.getNameAtBank() : null);
        metadata.put("name_match_score", cfResponse != null ? String.valueOf(cfResponse.getNameMatchScore()) : null);
        metadata.put("name_match_result", cfResponse != null ? cfResponse.getNameMatchResult() : null);
        metadata.put("verified_at", OffsetDateTime.now().toString());

        if (valid) {
            persistBankDetails(
                    organizationId,
                    landlord,
                    cfResponse.getBankAccount(),
                    cfResponse.getIfsc(),
                    null,
                    cfResponse.getNameAtBank() != null ? cfResponse.getNameAtBank() : name,
                    vpa,
                    "upi_verification",
                    metadata
            );
        }

        return BankVerificationResultResponse.builder()
                .valid(valid)
                .verificationMode("UPI_PENNY_DROP")
                .accountNumber(maskAccountNumber(cfResponse != null ? cfResponse.getBankAccount() : null))
                .ifsc(cfResponse != null ? cfResponse.getIfsc() : null)
                .upiId(vpa)
                .accountHolderName(cfResponse != null ? cfResponse.getNameAtBank() : name)
                .nameMatchScore(cfResponse != null && cfResponse.getNameMatchScore() != null ? String.valueOf(cfResponse.getNameMatchScore()) : null)
                .nameMatchResult(cfResponse != null ? cfResponse.getNameMatchResult() : null)
                .referenceId(cfResponse != null ? cfResponse.getRefIdAsString() : null)
                .message(cfResponse != null && cfResponse.getMessage() != null ? cfResponse.getMessage() : (valid ? "UPI ID verified successfully" : "UPI ID verification failed"))
                .linkedToLandlord(landlord != null)
                .metadata(metadata)
                .build();
    }

    private void persistBankDetails(
            UUID organizationId,
            Landlord landlord,
            String rawBankAccount,
            String ifsc,
            String bankName,
            String accountHolderName,
            String upiId,
            String metadataKey,
            Map<String, Object> verificationMetadata) {

        String encryptedAccount = rawBankAccount != null && !rawBankAccount.isBlank()
                ? piiEncryptionService.encrypt(rawBankAccount.trim())
                : null;
        String last4 = rawBankAccount != null && rawBankAccount.trim().length() >= 4
                ? rawBankAccount.trim().substring(rawBankAccount.trim().length() - 4)
                : rawBankAccount;

        if (landlord != null) {
            if (encryptedAccount != null) landlord.setBankAccountNumber(encryptedAccount);
            if (last4 != null) landlord.setBankAccountLast4(last4);
            if (ifsc != null && !ifsc.isBlank()) landlord.setBankIfscCode(ifsc.trim().toUpperCase());
            if (bankName != null && !bankName.isBlank()) landlord.setBankName(bankName.trim());
            if (accountHolderName != null && !accountHolderName.isBlank()) landlord.setAccountHolderName(accountHolderName.trim());
            if (upiId != null && !upiId.isBlank()) landlord.setOwnerUpiId(upiId.trim().toLowerCase());

            landlord.setMetadata(mergeMetadata(landlord.getMetadata(), metadataKey, verificationMetadata));
            landlordRepository.save(landlord);
            log.info("Persisted verified bank details for Landlord ID: {}", landlord.getId());
        } else if (organizationId != null) {
            BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(organizationId)
                    .orElseGet(() -> {
                        Organization org = organizationRepository.findById(organizationId).orElse(null);
                        return BrandingSettings.builder()
                                .organization(org)
                                .legalBusinessName(org != null ? org.getName() : "Organization")
                                .build();
                    });

            if (encryptedAccount != null) branding.setBankAccountNumber(encryptedAccount);
            if (ifsc != null && !ifsc.isBlank()) branding.setBankIfscCode(ifsc.trim().toUpperCase());
            if (bankName != null && !bankName.isBlank()) branding.setBankName(bankName.trim());
            if (accountHolderName != null && !accountHolderName.isBlank()) branding.setAccountHolderName(accountHolderName.trim());
            if (upiId != null && !upiId.isBlank()) branding.setOwnerUpiId(upiId.trim().toLowerCase());

            branding.setMetadata(mergeMetadata(branding.getMetadata(), metadataKey, verificationMetadata));
            brandingSettingsRepository.save(branding);
            log.info("Persisted verified bank details for Organization ID: {}", organizationId);
        }
    }

    private String maskAccountNumber(String account) {
        if (account == null || account.isBlank()) return null;
        String trimmed = account.trim();
        if (trimmed.length() <= 4) return trimmed;
        return "•".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
    }

    private UUID requireOrganization(SecurityUser currentUser) {
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            throw new BadRequestException("Active organization context is required for bank verification.");
        }
        return currentUser.getOrganizationId();
    }

    private Landlord resolveLandlord(UUID landlordId, UUID organizationId, SecurityUser currentUser) {
        if (landlordId == null) {
            return null;
        }
        Landlord landlord = landlordRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found: " + landlordId));

        if (!organizationId.equals(landlord.getManagingOrganization().getId())
                && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Landlord does not belong to the current organization context.");
        }
        return landlord;
    }

    private String mergeMetadata(String existingJson, String sectionKey, Map<String, Object> data) {
        try {
            Map<String, Object> root = (existingJson == null || existingJson.isBlank())
                    ? new HashMap<>()
                    : objectMapper.readValue(existingJson, new TypeReference<Map<String, Object>>() {});
            root.put(sectionKey, data);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to safely update verification metadata", e);
        }
    }

    private String trimToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
