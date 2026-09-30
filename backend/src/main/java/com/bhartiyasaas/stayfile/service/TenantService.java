package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;

public interface TenantService {
    TenantResponse createTenant(TenantCreateRequest request, SecurityUser currentUser);
    TenantResponse getTenantById(UUID id, SecurityUser currentUser);
    List<TenantResponse> getTenantsByOrganization(SecurityUser currentUser);
    TenantResponse verifyTenantKyc(UUID id, SecurityUser currentUser, VerificationStatus status);
}
