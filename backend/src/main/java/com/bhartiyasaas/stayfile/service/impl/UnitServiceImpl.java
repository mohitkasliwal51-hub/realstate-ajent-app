package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.UnitCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.UnitResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.UnitMapper;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.UnitService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final UnitMapper unitMapper;

    @Override
    @Transactional
    public UnitResponse createUnit(UnitCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Property property = propertyRepository.findByIdAndOrganizationId(request.getPropertyId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId()));

        Unit parentUnit = null;
        if (request.getParentUnitId() != null) {
            parentUnit = unitRepository.findByIdAndOrganizationId(request.getParentUnitId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent unit not found with ID: " + request.getParentUnitId()));
        }

        Unit unit = unitMapper.toEntity(request);
        unit.setOrganization(organization);
        unit.setProperty(property);
        unit.setParentUnit(parentUnit);
        if (request.getStatus() == null) {
            unit.setStatus(UnitStatus.AVAILABLE);
        }

        Unit savedUnit = unitRepository.save(unit);
        return unitMapper.toResponse(savedUnit);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnitById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Unit unit = unitRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + id));
        return unitMapper.toResponse(unit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> getUnitsByProperty(UUID propertyId, SecurityUser currentUser, UnitStatus statusFilter) {
        UUID organizationId = currentUser.getOrganizationId();
        List<Unit> units;
        if (statusFilter != null) {
            units = unitRepository.findByPropertyIdAndOrganizationIdAndStatus(propertyId, organizationId, statusFilter);
        } else {
            units = unitRepository.findByPropertyIdAndOrganizationId(propertyId, organizationId);
        }
        return unitMapper.toResponseList(units);
    }

    @Override
    @Transactional
    public UnitResponse updateUnitStatus(UUID id, SecurityUser currentUser, UnitStatus status) {
        UUID organizationId = currentUser.getOrganizationId();
        Unit unit = unitRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + id));

        unit.setStatus(status);
        Unit updatedUnit = unitRepository.save(unit);
        return unitMapper.toResponse(updatedUnit);
    }

    @Override
    @Transactional
    public UnitResponse updateUnit(UUID id, SecurityUser currentUser, UnitCreateRequest request) {
        UUID organizationId = currentUser.getOrganizationId();
        Unit unit = unitRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + id));

        unit.setUnitNumber(request.getUnitNumber());
        if (request.getFloorNumber() != null) unit.setFloorNumber(request.getFloorNumber());
        if (request.getSharingType() != null) unit.setSharingType(request.getSharingType());
        if (request.getMonthlyRent() != null) unit.setMonthlyRent(request.getMonthlyRent());
        if (request.getSecurityDeposit() != null) unit.setSecurityDeposit(request.getSecurityDeposit());
        if (request.getStatus() != null) unit.setStatus(request.getStatus());
        if (request.getAmenities() != null) unit.setAmenities(request.getAmenities());
        if (request.getNotes() != null) unit.setNotes(request.getNotes());

        if (request.getParentUnitId() != null) {
            Unit parentUnit = unitRepository.findByIdAndOrganizationId(request.getParentUnitId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent unit not found with ID: " + request.getParentUnitId()));
            unit.setParentUnit(parentUnit);
        }

        Unit updatedUnit = unitRepository.save(unit);
        return unitMapper.toResponse(updatedUnit);
    }

    @Override
    @Transactional
    public void deleteUnit(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Unit unit = unitRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + id));
        unitRepository.delete(unit);
    }
}
