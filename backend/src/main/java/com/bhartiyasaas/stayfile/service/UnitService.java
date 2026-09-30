package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.UnitCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.UnitResponse;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;

import java.util.List;
import java.util.UUID;

public interface UnitService {
    UnitResponse createUnit(UnitCreateRequest request);
    UnitResponse getUnitById(UUID id, UUID organizationId);
    List<UnitResponse> getUnitsByProperty(UUID propertyId, UUID organizationId, UnitStatus statusFilter);
    UnitResponse updateUnitStatus(UUID id, UUID organizationId, UnitStatus status);
    UnitResponse updateUnit(UUID id, UUID organizationId, UnitCreateRequest request);
    void deleteUnit(UUID id, UUID organizationId);
}
