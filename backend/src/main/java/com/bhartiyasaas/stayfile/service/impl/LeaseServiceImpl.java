package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.*;
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

    @Override
    @Transactional
    public LeaseResponse createLease(LeaseCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        AgreementTemplate template = null;
        if (request.getAgreementTemplateId() != null) {
            template = agreementTemplateRepository.findById(request.getAgreementTemplateId()).orElse(null);
        }

        Lease lease = Lease.builder()
                .organization(organization)
                .unit(unit)
                .tenant(tenant)
                .owner(owner)
                .agreementTemplate(template)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monthlyRent(request.getMonthlyRent())
                .securityDeposit(request.getSecurityDeposit())
                .rentDueDay(request.getRentDueDay() != null ? request.getRentDueDay() : 5)
                .noticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 30)
                .lockInPeriodMonths(request.getLockInPeriodMonths() != null ? request.getLockInPeriodMonths() : 6)
                .customClauses(request.getCustomClauses())
                .status(request.getStatus())
                .isEsignCompleted(false)
                .build();

        Lease savedLease = leaseRepository.save(lease);
        return mapToResponse(savedLease);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaseResponse getLeaseById(UUID id, UUID organizationId) {
        Lease lease = leaseRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + id));
        return mapToResponse(lease);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaseResponse> getLeasesByOrganization(UUID organizationId) {
        return leaseRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getLeasePdf(UUID leaseId, UUID organizationId) {
        Lease lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        return pdfGeneratorService.generateRentAgreementPdf(lease);
    }

    private LeaseResponse mapToResponse(Lease lease) {
        return LeaseResponse.builder()
                .id(lease.getId())
                .organizationId(lease.getOrganization().getId())
                .unitId(lease.getUnit().getId())
                .unitNumber(lease.getUnit().getUnitNumber())
                .tenantId(lease.getTenant().getId())
                .tenantName(lease.getTenant().getFullName())
                .ownerId(lease.getOwner().getId())
                .ownerName(lease.getOwner().getFullName())
                .startDate(lease.getStartDate())
                .endDate(lease.getEndDate())
                .monthlyRent(lease.getMonthlyRent())
                .securityDeposit(lease.getSecurityDeposit())
                .rentDueDay(lease.getRentDueDay())
                .noticePeriodDays(lease.getNoticePeriodDays())
                .lockInPeriodMonths(lease.getLockInPeriodMonths())
                .customClauses(lease.getCustomClauses())
                .status(lease.getStatus())
                .agreementPdfUrl(lease.getAgreementPdfUrl())
                .isEsignCompleted(lease.getIsEsignCompleted())
                .createdAt(lease.getCreatedAt())
                .updatedAt(lease.getUpdatedAt())
                .build();
    }
}
