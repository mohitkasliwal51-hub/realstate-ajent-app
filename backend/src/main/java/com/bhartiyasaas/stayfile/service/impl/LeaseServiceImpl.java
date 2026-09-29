package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.LeaseMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.LeaseService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaseServiceImpl implements LeaseService {

    private final LeaseRepository leaseRepository;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final ProfileRepository profileRepository;
    private final AgreementTemplateRepository agreementTemplateRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final LeaseMapper leaseMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public LeaseResponse createLease(LeaseCreateRequest request) {
        tenantAccessService.validateUserOrganization(request.getOrganizationId());

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        if (unit.getOrganization() == null || !unit.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Unit does not belong to the specified Organization");
        }
        if (tenant.getOrganization() == null || !tenant.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Tenant does not belong to the specified Organization");
        }
        if (owner.getOrganization() == null || !owner.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Owner profile does not belong to the specified Organization");
        }

        AgreementTemplate template = null;
        if (request.getAgreementTemplateId() != null) {
            template = agreementTemplateRepository.findByIdAndOrganizationId(request.getAgreementTemplateId(), organization.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agreement template not found or does not belong to your organization"));
        }

        Lease lease = leaseMapper.toEntity(request);
        lease.setOrganization(organization);
        lease.setUnit(unit);
        lease.setTenant(tenant);
        lease.setOwner(owner);
        lease.setAgreementTemplate(template);
        lease.setIsEsignCompleted(false);

        Lease savedLease = leaseRepository.save(lease);
        return leaseMapper.toResponse(savedLease);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaseResponse getLeaseById(UUID id, UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        Lease lease = leaseRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "rent agreement");
        return leaseMapper.toResponse(lease);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaseResponse> getLeasesByOrganization(UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        List<Lease> leases = leaseRepository.findByOrganizationId(organizationId).stream()
                .filter(lease -> tenantAccessService.canAccessTenant(lease.getTenant(), "rent agreement"))
                .collect(Collectors.toList());

        return leaseMapper.toResponseList(leases);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getLeasePdf(UUID leaseId, UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "rent agreement");
        return pdfGeneratorService.generateRentAgreementPdf(lease);
    }
}
