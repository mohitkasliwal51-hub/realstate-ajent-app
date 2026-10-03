package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.TenantMapper;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
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
    private final TenantMapper tenantMapper;
    private final TenantAccessService tenantAccessService;
    private final PiiEncryptionService piiEncryptionService;

    @Override
    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setOrganization(organization);

        if (request.getUserId() != null) {
            Profile user = profileRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant user profile not found with ID: " + request.getUserId()));
            if (user.getOrganization() == null || !user.getOrganization().getId().equals(organization.getId())) {
                throw new IllegalArgumentException("Tenant user profile does not belong to the specified Organization");
            }
            tenant.setUser(user);
        }

        // Process AES PII Encryption & ID proof fields in Service Layer
        if (request.getIdProofNumber() != null && !request.getIdProofNumber().isBlank()) {
            tenant.setIdProofNumber(piiEncryptionService.encrypt(request.getIdProofNumber()));
            if (request.getIdProofNumber().length() >= 4) {
                tenant.setIdProofLast4("XXXX-XXXX-" + request.getIdProofNumber().substring(request.getIdProofNumber().length() - 4));
            }
        }
        if (tenant.getIdProofType() == null || tenant.getIdProofType().isBlank()) {
            tenant.setIdProofType("Aadhaar");
        }
        tenant.setKycStatus(VerificationStatus.PENDING);

        Tenant savedTenant = tenantRepository.save(tenant);
        return tenantMapper.toResponse(savedTenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Tenant tenant = tenantRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(tenant, "tenant profile");
        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantForCurrentUser(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Tenant tenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for current user"));
        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getTenantsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        List<Tenant> tenants = tenantRepository.findByOrganizationId(organizationId).stream()
                .filter(tenant -> tenantAccessService.canAccessTenant(tenant, "tenant profile"))
                .collect(Collectors.toList());

        return tenantMapper.toResponseList(tenants);
    }

    @Override
    @Transactional
    public TenantResponse verifyTenantKyc(UUID id, SecurityUser currentUser, VerificationStatus status) {
        UUID organizationId = currentUser.getOrganizationId();
        Tenant tenant = tenantRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
        tenant.setKycStatus(status);
        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }
}
