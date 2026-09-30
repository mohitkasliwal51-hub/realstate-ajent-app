package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.UnitCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.UnitResponse;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface UnitService {
    UnitResponse createUnit(UnitCreateRequest request, SecurityUser currentUser);
    UnitResponse getUnitById(UUID id, SecurityUser currentUser);
    List<UnitResponse> getUnitsByProperty(UUID propertyId, SecurityUser currentUser, UnitStatus statusFilter);
    UnitResponse updateUnitStatus(UUID id, SecurityUser currentUser, UnitStatus status);
    UnitResponse updateUnit(UUID id, SecurityUser currentUser, UnitCreateRequest request);
    void deleteUnit(UUID id, SecurityUser currentUser);
}
