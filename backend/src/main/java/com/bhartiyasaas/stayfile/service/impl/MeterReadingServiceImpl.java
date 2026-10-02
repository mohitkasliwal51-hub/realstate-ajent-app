package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.MeterReadingRequest;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.entity.MeterReading;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.MeterReadingMapper;
import com.bhartiyasaas.stayfile.repository.MeterReadingRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.repository.LeaseRepository;
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
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public MeterReadingResponse createMeterReading(MeterReadingRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Unit unit = unitRepository.findByIdAndOrganizationId(request.getUnitId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + request.getUnitId()));

        String meterType = request.getMeterType() == null || request.getMeterType().isBlank()
            ? "ELECTRICITY" : request.getMeterType().toUpperCase();
        BigDecimal previous = request.getPreviousReading();
        if (previous == null) {
            previous = meterReadingRepository
                .findTopByUnitIdAndMeterTypeOrderByReadingDateDesc(request.getUnitId(), meterType)
                    .map(MeterReading::getCurrentReading)
                    .orElse(BigDecimal.ZERO);
        }
        if (request.getCurrentReading().compareTo(previous) < 0) {
            throw new IllegalArgumentException("Current meter reading cannot be lower than previous reading");
        }

        MeterReading meterReading = meterReadingMapper.toEntity(request);
        meterReading.setOrganization(organization);
        meterReading.setUnit(unit);
        meterReading.setMeterType(meterType);
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
        return meterReadingMapper.toResponse(meterReading);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByUnit(UUID unitId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        unitRepository.findByIdAndOrganizationId(unitId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + unitId));
        return meterReadingMapper.toResponseList(meterReadingRepository.findByUnitId(unitId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMeterReadingsByLease(UUID leaseId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        var lease = leaseRepository.findByIdAndOrganizationId(leaseId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Lease not found with ID: " + leaseId));
        tenantAccessService.validateTenantOwnership(lease.getTenant(), "meter readings");
        return meterReadingMapper.toResponseList(meterReadingRepository.findByUnitId(lease.getUnit().getId()));
    }
}
