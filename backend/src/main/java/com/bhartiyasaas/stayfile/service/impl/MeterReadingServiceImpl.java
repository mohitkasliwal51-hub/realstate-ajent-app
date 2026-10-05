package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.MeterReadingRequest;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.entity.MeterReading;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.MeterReadingMapper;
import com.bhartiyasaas.stayfile.repository.MeterReadingRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.service.MeterReadingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final UnitRepository unitRepository;
    private final OrganizationRepository organizationRepository;
    private final MeterReadingMapper meterReadingMapper;
    private final LeaseRepository leaseRepository;
    private final com.bhartiyasaas.stayfile.repository.TenantRepository tenantRepository;
    private final TenantAccessService tenantAccessService;

    private final PropertyRepository propertyRepository;

    @Override
    @Transactional
    public MeterReadingResponse createMeterReading(MeterReadingRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Unit unit = null;
        Property property = null;

        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationId(request.getUnitId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));
            property = unit.getProperty();
            if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
                validateTenantUnitAccess(unit.getId(), currentUser);
            }
        } else if (request.getPropertyId() != null) {
            if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
                throw new org.springframework.security.access.AccessDeniedException("Tenants can only submit sub-meter readings for their assigned unit");
            }
            property = propertyRepository.findByIdAndOrganizationId(request.getPropertyId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId()));
        } else {
            throw new com.bhartiyasaas.stayfile.exception.BadRequestException("Either Unit ID or Property ID is required for a meter reading");
        }

        String meterType = request.getMeterType() == null || request.getMeterType().isBlank()
            ? "ELECTRICITY" : request.getMeterType().toUpperCase();
        String meterScope = request.getMeterScope() == null || request.getMeterScope().isBlank()
            ? (unit != null ? "ROOM_AC" : "MASTER_DISCOM") : request.getMeterScope().toUpperCase();

        BigDecimal previous = request.getPreviousReading();
        if (previous == null) {
            if (unit != null) {
                previous = meterReadingRepository
                    .findTopByUnitIdAndOrganizationIdAndMeterTypeOrderByReadingDateDesc(
                        unit.getId(), organizationId, meterType)
                        .map(MeterReading::getCurrentReading)
                        .orElse(BigDecimal.ZERO);
            } else {
                previous = BigDecimal.ZERO;
            }
        }
        if (request.getCurrentReading().compareTo(previous) < 0) {
            throw new IllegalArgumentException("Current meter reading cannot be lower than previous reading");
        }

        MeterReading meterReading = meterReadingMapper.toEntity(request);
        meterReading.setOrganization(organization);
        meterReading.setProperty(property);
        meterReading.setUnit(unit);
        meterReading.setMeterType(meterType);
        meterReading.setMeterScope(meterScope);
        meterReading.setPhotoUrl(request.getPhotoUrl());
        meterReading.setSubmittedByUserId(currentUser.getId());
        meterReading.setVerificationStatus(currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT
                ? "PENDING_VERIFICATION" : "APPROVED");
        meterReading.setPreviousReading(previous);
        meterReading.setCurrentReading(request.getCurrentReading());
        meterReading.setRatePerUnit(request.getRatePerUnit() != null ? request.getRatePerUnit() : BigDecimal.valueOf(10.00));
        meterReading.setReadingDate(request.getReadingDate() != null ? request.getReadingDate() : LocalDate.now());
        meterReading.setIsBilled(false);

        MeterReading saved = meterReadingRepository.save(meterReading);
        return meterReadingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterReadingResponse getMeterReadingById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        MeterReading meterReading = meterReadingRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with ID: " + id));
        if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
            validateTenantUnitAccess(meterReading.getUnit().getId(), currentUser);
        }
        return meterReadingMapper.toResponse(meterReading);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByUnit(UUID unitId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        unitRepository.findByIdAndOrganizationId(unitId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + unitId));
        if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
            validateTenantUnitAccess(unitId, currentUser);
        }
        return meterReadingMapper.toResponseList(meterReadingRepository.findByUnitIdAndOrganizationId(unitId, organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByProperty(UUID propertyId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        propertyRepository.findByIdAndOrganizationId(propertyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));
        return meterReadingMapper.toResponseList(meterReadingRepository.findByPropertyIdAndOrganizationId(propertyId, organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByLease(UUID leaseId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        var lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "meter readings");
        return meterReadingMapper.toResponseList(meterReadingRepository.findByUnitIdAndOrganizationId(
            lease.getUnit().getId(), organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        return meterReadingMapper.toResponseList(meterReadingRepository.findByOrganizationIdOrderByReadingDateDesc(organizationId));
    }

    @Override
    @Transactional
    public MeterReadingResponse verifyMeterReading(UUID id, String status, String disputeReason, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        MeterReading meterReading = meterReadingRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with ID: " + id));

        if (currentUser.getRole() == com.bhartiyasaas.stayfile.entity.enums.UserRole.TENANT) {
            if (meterReading.getUnit() != null) {
                validateTenantUnitAccess(meterReading.getUnit().getId(), currentUser);
            }
        }

        String verifiedStatus = "DISPUTED".equalsIgnoreCase(status) ? "DISPUTED" : "APPROVED";
        meterReading.setVerificationStatus(verifiedStatus);
        if ("DISPUTED".equals(verifiedStatus)) {
            meterReading.setDisputeReason(disputeReason != null ? disputeReason : "Disputed by resident");
        } else {
            meterReading.setDisputeReason(null);
        }

        MeterReading saved = meterReadingRepository.save(meterReading);
        return meterReadingMapper.toResponse(saved);
    }

    private void validateTenantUnitAccess(UUID unitId, SecurityUser currentUser) {
        var tenantOpt = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), currentUser.getOrganizationId());
        if (tenantOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: No tenant profile found");
        }
        boolean hasLeaseOnUnit = leaseRepository.findByTenantId(tenantOpt.get().getId()).stream()
                .anyMatch(lease -> lease.getUnit() != null && lease.getUnit().getId().equals(unitId));
        if (!hasLeaseOnUnit) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only view meter readings for your assigned unit");
        }
    }
}
