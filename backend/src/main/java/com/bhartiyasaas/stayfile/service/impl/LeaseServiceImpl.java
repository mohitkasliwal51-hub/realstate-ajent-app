package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.exception.UnauthorizedException;
import com.bhartiyasaas.stayfile.mapper.LeaseMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.SecurityUser;
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
    private final LandlordRepository landlordRepository;
    private final ProfileRepository profileRepository;
    private final AgreementTemplateRepository agreementTemplateRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final LeaseMapper leaseMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public LeaseResponse createLease(LeaseCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Lease end date must be on or after the start date");
        }

        Unit unit = unitRepository.findByIdForUpdate(request.getUnitId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));

        if (unit.getOrganization() == null || !unit.getOrganization().getId().equals(organizationId)) {
            throw new UnauthorizedException("Unit does not belong to your Organization");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        if (tenant.getOrganization() == null || !tenant.getOrganization().getId().equals(organizationId)) {
            throw new UnauthorizedException("Tenant does not belong to your Organization");
        }

        Landlord landlord = null;
        if (request.getLandlordId() != null) {
            landlord = landlordRepository.findByIdAndManagingOrganizationId(request.getLandlordId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + request.getLandlordId()));
        }

        UUID creatorId = request.getCreatedById() != null ? request.getCreatedById() : currentUser.getProfileId();
        Profile createdBy = profileRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator profile not found with ID: " + creatorId));

        // Rule 1: Prevent leasing occupied, maintenance or disabled units
        if (unit.getStatus() == UnitStatus.OCCUPIED || unit.getStatus() == UnitStatus.MAINTENANCE || unit.getStatus() == UnitStatus.DISABLED) {
            throw new BadRequestException("Unit " + unit.getUnitNumber() + " is currently " + unit.getStatus() + " and cannot be leased.");
        }

        // Rule 2: Prevent overlapping leases
        List<Lease> overlapping = leaseRepository.findOverlappingLeases(
            unit.getId(), organizationId, request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Unit already booked for selected dates");
        }

        AgreementTemplate template = null;
        if (request.getAgreementTemplateId() != null) {
            template = agreementTemplateRepository.findByIdAndOrganizationId(request.getAgreementTemplateId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agreement template not found or does not belong to your organization"));
        }

        Lease lease = leaseMapper.toEntity(request);
        lease.setOrganization(organization);
        lease.setUnit(unit);
        lease.setTenant(tenant);
        lease.setLandlord(landlord);
        lease.setCreatedBy(createdBy);
        lease.setAgreementTemplate(template);
        lease.setIsEsignCompleted(false);
        if (request.getStatus() != null) {
            lease.setStatus(request.getStatus());
        } else {
            lease.setStatus(LeaseStatus.DRAFT);
        }

        Lease savedLease = leaseRepository.save(lease);

        if (savedLease.getStatus() == LeaseStatus.ACTIVE) {
            unit.setStatus(UnitStatus.OCCUPIED);
            unit.setCurrentLease(savedLease);
            unitRepository.save(unit);
        }

        return leaseMapper.toResponse(savedLease);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaseResponse getLeaseById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Lease lease = leaseRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + id));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "rent agreement");
        return leaseMapper.toResponse(lease);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaseResponse> getLeasesByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        List<Lease> leases = leaseRepository.findByOrganizationId(organizationId).stream()
                .filter(lease -> tenantAccessService.canAccessTenant(lease.getTenant(), "rent agreement"))
                .collect(Collectors.toList());

        return leaseMapper.toResponseList(leases);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getLeasePdf(UUID leaseId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "rent agreement");
        return pdfGeneratorService.generateRentAgreementPdf(lease);
    }

    @Override
    @Transactional
    public LeaseResponse updateLeaseStatus(UUID leaseId, SecurityUser currentUser, LeaseStatus targetStatus) {
        UUID organizationId = currentUser.getOrganizationId();
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
