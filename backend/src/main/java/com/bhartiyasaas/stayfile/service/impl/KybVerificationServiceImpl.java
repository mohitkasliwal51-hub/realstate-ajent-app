package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.request.CinVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.GstinVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.PanToGstinRequest;
import com.bhartiyasaas.stayfile.dto.response.CinVerificationResponse;
import com.bhartiyasaas.stayfile.dto.response.GstinVerificationResponse;
import com.bhartiyasaas.stayfile.dto.response.PanToGstinResponse;
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
import com.bhartiyasaas.stayfile.service.KybVerificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KybVerificationServiceImpl implements KybVerificationService {
    private final CashfreeHttpClient cashfreeHttpClient;
    private final LandlordRepository landlordRepository;
    private final BrandingSettingsRepository brandingSettingsRepository;
    private final OrganizationRepository organizationRepository;
    private final PiiEncryptionService piiEncryptionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public GstinVerificationResponse verifyGstin(GstinVerificationRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);
        String gstin = request.getGstin().trim().toUpperCase();
        CashfreeGstinResponse cf = cashfreeHttpClient.post(
                "/gstin",
                CashfreeGstinRequest.builder().gstin(gstin).businessName(trimToNull(request.getBusinessName())).build(),
                CashfreeGstinResponse.class, organizationId);
        boolean valid = cf.isValid();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gstin", gstin);
        metadata.put("valid", valid);
        metadata.put("status", cf.getStatus());
        metadata.put("legal_name", cf.getLegalName());
        metadata.put("trade_name", cf.getTradeName());
        metadata.put("registration_date", cf.getRegistrationDate());
        metadata.put("taxpayer_type", cf.getTaxpayerType());
        metadata.put("verified_at", OffsetDateTime.now().toString());
        if (landlord != null) {
            if (valid) landlord.setGstin(gstin);
            landlord.setMetadata(mergeMetadata(landlord.getMetadata(), "gstin_verification", metadata));
            landlordRepository.save(landlord);
        } else if (organizationId != null && valid) {
            BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(organizationId)
                    .orElseGet(() -> {
                        Organization org = organizationRepository.findById(organizationId).orElse(null);
                        return BrandingSettings.builder()
                                .organization(org)
                                .legalBusinessName(org != null ? org.getName() : "Organization")
                                .build();
                    });
            branding.setOwnerGstin(gstin);
            if (cf.getLegalName() != null && !cf.getLegalName().isBlank()) {
                branding.setLegalBusinessName(cf.getLegalName());
            }
            if (cf.getTradeName() != null && !cf.getTradeName().isBlank()) {
                branding.setTradeName(cf.getTradeName());
            }
            if (cf.getAddress() != null && !cf.getAddress().isBlank()) {
                branding.setRegisteredOfficeAddress(cf.getAddress());
            }
            branding.setMetadata(mergeMetadata(branding.getMetadata(), "gstin_verification", metadata));
            brandingSettingsRepository.save(branding);
        }
        return GstinVerificationResponse.builder().valid(valid).gstin(gstin)
                .legalName(cf.getLegalName()).tradeName(cf.getTradeName())
                .registrationDate(cf.getRegistrationDate()).constitution(cf.getConstitution())
                .taxpayerType(cf.getTaxpayerType()).status(cf.getStatus())
                .centerJurisdiction(cf.getCenterJurisdiction()).stateJurisdiction(cf.getStateJurisdiction())
                .lastUpdateDate(cf.getLastUpdateDate()).address(cf.getAddress())
                .activities(cf.getActivities()).additionalAddresses(cf.getAdditionalAddresses())
                .metadata(cf.getSplitAddress()).referenceId(cf.getReferenceIdAsString())
                .message(cf.getMessage() != null ? cf.getMessage() : (valid ? "GSTIN is valid" : "GSTIN is invalid"))
                .linkedToLandlord(landlord != null).build();
    }

    @Override
    @Transactional
    public PanToGstinResponse findGstinsByPan(PanToGstinRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);
        String pan = request.getPan().trim().toUpperCase();
        String verificationId = "sf_kyb_pan_" + UUID.randomUUID().toString().replace("-", "");
        CashfreePanToGstinResponse cf = cashfreeHttpClient.post(
                "/pan-gstin",
                CashfreePanToGstinRequest.builder().pan(pan).verificationId(verificationId).build(),
                CashfreePanToGstinResponse.class, organizationId);
        List<PanToGstinResponse.GstinRecord> gstins = cf.getGstins() == null ? List.of()
                : cf.getGstins().stream().map(g -> PanToGstinResponse.GstinRecord.builder()
                .gstin(g.getGstin()).status(g.getStatus()).state(g.getState()).build()).toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pan_last4", pan.substring(pan.length() - 4));
        metadata.put("status", cf.getStatus());
        metadata.put("gstins", gstins);
        metadata.put("verified_at", OffsetDateTime.now().toString());
        if (landlord != null) {
            landlord.setPan(piiEncryptionService.encrypt(pan));
            landlord.setPanLast4(pan.substring(pan.length() - 4));
            landlord.setMetadata(mergeMetadata(landlord.getMetadata(), "pan_to_gstin", metadata));
            landlordRepository.save(landlord);
        } else if (organizationId != null) {
            BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(organizationId)
                    .orElseGet(() -> {
                        Organization org = organizationRepository.findById(organizationId).orElse(null);
                        return BrandingSettings.builder()
                                .organization(org)
                                .legalBusinessName(org != null ? org.getName() : "Organization")
                                .build();
                    });
            branding.setOwnerPan(pan);
            branding.setMetadata(mergeMetadata(branding.getMetadata(), "pan_to_gstin", metadata));
            brandingSettingsRepository.save(branding);
        }
        return PanToGstinResponse.builder().pan("******" + pan.substring(pan.length() - 4))
                .verificationId(cf.getVerificationId())
                .status(cf.getStatus()).gstins(gstins).referenceId(cf.getReferenceIdAsString())
                .linkedToLandlord(landlord != null).build();
    }

    @Override
    @Transactional
    public CinVerificationResponse verifyCin(CinVerificationRequest request, SecurityUser currentUser) {
        UUID organizationId = requireOrganization(currentUser);
        Landlord landlord = resolveLandlord(request.getLandlordId(), organizationId, currentUser);
        String cin = request.getCin().trim().toUpperCase();
        CashfreeCinResponse cf = cashfreeHttpClient.post(
                "/cin",
                CashfreeCinRequest.builder().cin(cin).verificationId("sf_kyb_cin_" + UUID.randomUUID().toString().replace("-", "")).build(),
                CashfreeCinResponse.class, organizationId);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("cin", cin);
        metadata.put("status", cf.getStatus());
        metadata.put("company_name", cf.getCompanyName());
        metadata.put("cin_status", cf.getCompanyStatus());
        metadata.put("verified_at", OffsetDateTime.now().toString());
        if (landlord != null) {
            landlord.setMetadata(mergeMetadata(landlord.getMetadata(), "cin_verification", metadata));
            landlordRepository.save(landlord);
        } else if (organizationId != null) {
            BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(organizationId)
                    .orElseGet(() -> {
                        Organization org = organizationRepository.findById(organizationId).orElse(null);
                        return BrandingSettings.builder()
                                .organization(org)
                                .legalBusinessName(org != null ? org.getName() : "Organization")
                                .build();
                    });
            if (cf.getCompanyName() != null && !cf.getCompanyName().isBlank()) {
                branding.setLegalBusinessName(cf.getCompanyName());
            }
            branding.setMetadata(mergeMetadata(branding.getMetadata(), "cin_verification", metadata));
            brandingSettingsRepository.save(branding);
        }
        return CinVerificationResponse.builder().valid(cf.isValid()).cin(cin).verificationId(cf.getVerificationId()).companyName(cf.getCompanyName())
                .registrationNumber(cf.getRegistrationNumber()).incorporationDate(cf.getIncorporationDate())
                .companyStatus(cf.getCompanyStatus()).email(cf.getEmail()).incorporationCountry(cf.getIncorporationCountry())
                .directors(cf.getDirectors()).referenceId(cf.getReferenceIdAsString())
                .linkedToLandlord(landlord != null).build();
    }

    private UUID requireOrganization(SecurityUser user) {
        if (user.getRole() == UserRole.TENANT) throw new AccessDeniedException("Tenants are not permitted to perform KYB");
        UUID organizationId = user.getOrganizationId();
        if (organizationId == null && user.getRole() != UserRole.SUPER_ADMIN)
            throw new AccessDeniedException("Organization context is required for KYB verification");
        return organizationId;
    }

    private Landlord resolveLandlord(UUID landlordId, UUID organizationId, SecurityUser user) {
        if (landlordId == null) return null;
        if (organizationId == null) throw new AccessDeniedException("Organization context is required to link KYB");
        return landlordRepository.findByIdAndManagingOrganizationId(landlordId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + landlordId));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String mergeMetadata(String existing, String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        try {
            if (existing != null && !existing.isBlank())
                map.putAll(objectMapper.readValue(existing, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception ex) {
            throw new BadRequestException("Existing landlord metadata is invalid");
        }
        map.put(key, value);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist KYB metadata", ex);
        }
    }
}
