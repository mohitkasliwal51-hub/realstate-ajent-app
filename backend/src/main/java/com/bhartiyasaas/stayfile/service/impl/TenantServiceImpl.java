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
import com.bhartiyasaas.stayfile.mapper.TenantMapper;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
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

    @Override
    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        if (owner.getOrganization() != null && !owner.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Owner profile does not belong to the specified Organization");
        }

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setOrganization(organization);
        tenant.setOwner(owner);

        // Process PII & ID proof fields in Service Layer
        if (request.getIdProofNumber() != null && !request.getIdProofNumber().isBlank()) {
            tenant.setIdProofNumber("enc:" + request.getIdProofNumber());
            if (request.getIdProofNumber().length() >= 4) {
                tenant.setIdProofLast4(request.getIdProofNumber().substring(request.getIdProofNumber().length() - 4));
            }
        }
        if (tenant.getIdProofType() == null || tenant.getIdProofType().isBlank()) {
            tenant.setIdProofType("Aadhaar");
        }
        if (tenant.getIsIdVerified() == null) {
            tenant.setIsIdVerified(false);
        }

        Tenant savedTenant = tenantRepository.save(tenant);
        return tenantMapper.toResponse(savedTenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID id, UUID organizationId) {
        Tenant tenant = tenantRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(tenant, "tenant profile");
        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getTenantsByOrganization(UUID organizationId) {
        List<Tenant> tenants = tenantRepository.findByOrganizationId(organizationId).stream()
                .filter(tenant -> tenantAccessService.canAccessTenant(tenant, "tenant profile"))
                .collect(Collectors.toList());

        return tenantMapper.toResponseList(tenants);
    }
}
