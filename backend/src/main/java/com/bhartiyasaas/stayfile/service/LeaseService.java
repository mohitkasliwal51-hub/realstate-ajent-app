package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;

public interface LeaseService {
    LeaseResponse createLease(LeaseCreateRequest request);
    LeaseResponse getLeaseById(UUID id, UUID organizationId);
    List<LeaseResponse> getLeasesByOrganization(UUID organizationId);
    byte[] getLeasePdf(UUID leaseId, UUID organizationId);
}
