package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;

public interface TenantService {
    TenantResponse createTenant(TenantCreateRequest request);
    TenantResponse getTenantById(UUID id, UUID organizationId);
    List<TenantResponse> getTenantsByOrganization(UUID organizationId);
}
