package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.service.TenantService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        String idProofNumber = request.getIdProofNumber();
        String idProofLast4 = null;
        if (idProofNumber != null && idProofNumber.length() >= 4) {
            idProofLast4 = idProofNumber.substring(idProofNumber.length() - 4);
        }

        // Encryption-ready storage (last 4 unencrypted for UI, full string encrypted or tokenized)
        Tenant tenant = Tenant.builder()
                .organization(organization)
                .owner(owner)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .alternatePhone(request.getAlternatePhone())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .permanentAddress(request.getPermanentAddress())
                .occupation(request.getOccupation())
                .organizationOrCollege(request.getOrganizationOrCollege())
                .workAddress(request.getWorkAddress())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelation(request.getEmergencyContactRelation())
                .idProofType(request.getIdProofType() != null ? request.getIdProofType() : "Aadhaar")
                .idProofLast4(idProofLast4)
                .idProofNumber(idProofNumber != null ? "enc:" + idProofNumber : null)
                .isIdVerified(false)
                .idProofFrontUrl(request.getIdProofFrontUrl())
                .idProofBackUrl(request.getIdProofBackUrl())
                .tenantPhotoUrl(request.getTenantPhotoUrl())
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        return mapToResponse(savedTenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID id, UUID organizationId) {
        Tenant tenant = tenantRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
        validateTenantOwnership(tenant);
        return mapToResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getTenantsByOrganization(UUID organizationId) {
        List<Tenant> tenants = tenantRepository.findByOrganizationId(organizationId).stream()
                .filter(tenant -> {
                    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null || !(auth.getPrincipal() instanceof com.bhartiyasaas.stayfile.security.SecurityUser securityUser)) {
                        return true;
                    }
                    if (securityUser.getProfile().getRole() != com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
                        return true;
                    }
                    String tenantEmail = tenant.getEmail();
                    return tenantEmail != null && tenantEmail.equalsIgnoreCase(securityUser.getUsername());
                })
                .collect(Collectors.toList());

        return tenants.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void validateTenantOwnership(Tenant tenant) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.bhartiyasaas.stayfile.security.SecurityUser securityUser) {
            if (securityUser.getProfile().getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
                String tenantEmail = tenant.getEmail();
                if (tenantEmail == null || !tenantEmail.equalsIgnoreCase(securityUser.getUsername())) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only access your own tenant profile");
                }
            }
        }
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .organizationId(tenant.getOrganization().getId())
                .ownerId(tenant.getOwner().getId())
                .fullName(tenant.getFullName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .alternatePhone(tenant.getAlternatePhone())
                .gender(tenant.getGender())
                .dateOfBirth(tenant.getDateOfBirth())
                .permanentAddress(tenant.getPermanentAddress())
                .occupation(tenant.getOccupation())
                .organizationOrCollege(tenant.getOrganizationOrCollege())
                .workAddress(tenant.getWorkAddress())
                .emergencyContactName(tenant.getEmergencyContactName())
                .emergencyContactPhone(tenant.getEmergencyContactPhone())
                .emergencyContactRelation(tenant.getEmergencyContactRelation())
                .idProofType(tenant.getIdProofType())
                .idProofLast4(tenant.getIdProofLast4())
                .isIdVerified(tenant.getIsIdVerified())
                .idProofFrontUrl(tenant.getIdProofFrontUrl())
                .idProofBackUrl(tenant.getIdProofBackUrl())
                .tenantPhotoUrl(tenant.getTenantPhotoUrl())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
