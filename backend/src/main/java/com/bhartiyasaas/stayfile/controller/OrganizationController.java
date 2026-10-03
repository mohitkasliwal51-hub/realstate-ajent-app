package com.bhartiyasaas.stayfile.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.dto.response.ShowcaseBrandingResponse;
import com.bhartiyasaas.stayfile.entity.BrandingSettings;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.PropertyService;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final BrandingSettingsRepository brandingSettingsRepository;
    private final PropertyService propertyService;
    private final TenantAccessService tenantAccessService;

    @Data
    @Builder
    public static class OrganizationDetailsResponse {
        private UUID id;
        private String name;
        private String slug;
        private Boolean isActive;
        private ShowcaseBrandingResponse branding;
    }

    @Data
    @Builder
    public static class ShowcaseResponse {
        private String organizationName;
        private String organizationSlug;
        private ShowcaseBrandingResponse branding;
        private List<PropertyResponse> properties;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @GetMapping("/organizations/{id}")
    public ResponseEntity<ApiResponse<OrganizationDetailsResponse>> getOrganizationDetails(@PathVariable UUID id) {
        tenantAccessService.validateUserOrganization(id);
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(id).orElse(null);

        OrganizationDetailsResponse response = OrganizationDetailsResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .isActive(org.getIsActive())
                .branding(toPublicBranding(branding))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PutMapping("/organizations/{id}/branding")
        public ResponseEntity<ApiResponse<ShowcaseBrandingResponse>> updateBranding(
            @PathVariable UUID id,
            @RequestBody BrandingSettings brandingPayload) {
        tenantAccessService.validateUserOrganization(id);
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(id)
                .orElseGet(() -> BrandingSettings.builder().organization(org).legalBusinessName(org.getName()).build());

        if (brandingPayload.getLegalBusinessName() != null) branding.setLegalBusinessName(brandingPayload.getLegalBusinessName());
        if (brandingPayload.getTradeName() != null) branding.setTradeName(brandingPayload.getTradeName());
        if (brandingPayload.getContactPhone() != null) branding.setContactPhone(brandingPayload.getContactPhone());
        if (brandingPayload.getContactEmail() != null) branding.setContactEmail(brandingPayload.getContactEmail());
        if (brandingPayload.getAgencyLogoUrl() != null) branding.setAgencyLogoUrl(brandingPayload.getAgencyLogoUrl());
        if (brandingPayload.getPrimaryColor() != null) branding.setPrimaryColor(brandingPayload.getPrimaryColor());
        if (brandingPayload.getSecondaryColor() != null) branding.setSecondaryColor(brandingPayload.getSecondaryColor());
        if (brandingPayload.getRegisteredOfficeAddress() != null) branding.setRegisteredOfficeAddress(brandingPayload.getRegisteredOfficeAddress());
        if (brandingPayload.getOwnerPan() != null) branding.setOwnerPan(brandingPayload.getOwnerPan());
        if (brandingPayload.getOwnerGstin() != null) branding.setOwnerGstin(brandingPayload.getOwnerGstin());
        if (brandingPayload.getReraNumber() != null) branding.setReraNumber(brandingPayload.getReraNumber());

        BrandingSettings saved = brandingSettingsRepository.save(branding);
        return ResponseEntity.ok(ApiResponse.success(toPublicBranding(saved), "Branding settings updated successfully"));
    }

    @GetMapping("/showcase/{organizationSlug}")
    public ResponseEntity<ApiResponse<ShowcaseResponse>> getPublicShowcase(@PathVariable String organizationSlug) {
        Organization org = organizationRepository.findBySlug(organizationSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with slug: " + organizationSlug));

        BrandingSettings branding = brandingSettingsRepository.findByOrganizationId(org.getId()).orElse(null);
        List<PropertyResponse> properties = propertyService.getPublicPropertiesByOrganization(org.getId());

        ShowcaseBrandingResponse publicBranding = branding == null ? null : ShowcaseBrandingResponse.builder()
                .legalBusinessName(branding.getLegalBusinessName())
                .tradeName(branding.getTradeName())
                .ownerGstin(branding.getOwnerGstin())
                .contactPhone(branding.getContactPhone())
                .contactEmail(branding.getContactEmail())
                .agencyLogoUrl(branding.getAgencyLogoUrl())
                .primaryColor(branding.getPrimaryColor())
                .secondaryColor(branding.getSecondaryColor())
                .build();

        ShowcaseResponse showcase = ShowcaseResponse.builder()
                .organizationName(org.getName())
                .organizationSlug(org.getSlug())
                .branding(publicBranding)
                .properties(properties)
                .build();

        return ResponseEntity.ok(ApiResponse.success(showcase, "Showcase data retrieved successfully"));
        }

        private ShowcaseBrandingResponse toPublicBranding(BrandingSettings branding) {
                if (branding == null) {
                        return null;
                }
                return ShowcaseBrandingResponse.builder()
                                .legalBusinessName(branding.getLegalBusinessName())
                                .tradeName(branding.getTradeName())
                                .ownerGstin(branding.getOwnerGstin())
                                .contactPhone(branding.getContactPhone())
                                .contactEmail(branding.getContactEmail())
                                .agencyLogoUrl(branding.getAgencyLogoUrl())
                                .primaryColor(branding.getPrimaryColor())
                                .secondaryColor(branding.getSecondaryColor())
                                .build();
        }
}
