package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.LeaseMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.LeaseService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Lease end date must be on or after the start date");
        }

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

        // Rule 1: Prevent leasing occupied, maintenance or disabled units
        if (unit.getStatus() == UnitStatus.OCCUPIED || unit.getStatus() == UnitStatus.MAINTENANCE || unit.getStatus() == UnitStatus.DISABLED) {
            throw new IllegalStateException("Unit " + unit.getUnitNumber() + " is currently " + unit.getStatus() + " and cannot be leased.");
        }

        // Rule 2: Prevent overlapping leases
        Set<LeaseStatus> activeStatuses = EnumSet.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_ESIGN, LeaseStatus.ACTIVE);
        List<Lease> overlapping = leaseRepository.findOverlappingLeases(
                unit.getId(), request.getStartDate(), request.getEndDate(), activeStatuses);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("An active, pending, or draft lease already exists for unit " + unit.getUnitNumber() + " during the selected date range.");
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
        lease.setStatus(LeaseStatus.DRAFT);

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

    @Override
    @Transactional
    public LeaseResponse updateLeaseStatus(UUID leaseId, UUID organizationId, LeaseStatus targetStatus) {
        tenantAccessService.validateUserOrganization(organizationId);
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));

        LeaseStatus currentStatus = lease.getStatus();
        validateStatusTransition(currentStatus, targetStatus);

        lease.setStatus(targetStatus);
        Unit unit = lease.getUnit();

        // Handle Unit status changes depending on state
        if (targetStatus == LeaseStatus.ACTIVE) {
            unit.setStatus(UnitStatus.OCCUPIED);
            unit.setCurrentLease(lease);
            unitRepository.save(unit);
        } else if (targetStatus == LeaseStatus.EXPIRED || targetStatus == LeaseStatus.TERMINATED || targetStatus == LeaseStatus.CANCELLED) {
            if (unit.getCurrentLease() != null && unit.getCurrentLease().getId().equals(lease.getId())) {
                unit.setCurrentLease(null);
                unit.setStatus(UnitStatus.AVAILABLE);
                unitRepository.save(unit);
            }
        }

        Lease updatedLease = leaseRepository.save(lease);
        return leaseMapper.toResponse(updatedLease);
    }

    private void validateStatusTransition(LeaseStatus current, LeaseStatus target) {
        if (current == target) return;

        switch (current) {
            case DRAFT -> {
                if (target != LeaseStatus.PENDING_ESIGN && target != LeaseStatus.ACTIVE && target != LeaseStatus.CANCELLED) {
                    throw new IllegalStateException("Cannot transition from DRAFT to " + target);
                }
            }
            case PENDING_ESIGN -> {
                if (target != LeaseStatus.ACTIVE && target != LeaseStatus.CANCELLED) {
                    throw new IllegalStateException("Cannot transition from PENDING_ESIGN to " + target);
                }
            }
            case ACTIVE -> {
                if (target != LeaseStatus.EXPIRED && target != LeaseStatus.TERMINATED) {
                    throw new IllegalStateException("Cannot transition from ACTIVE to " + target);
                }
            }
            case EXPIRED, TERMINATED, CANCELLED -> {
                throw new IllegalStateException("Terminal state " + current + " cannot be changed.");
            }
        }
    }
}
