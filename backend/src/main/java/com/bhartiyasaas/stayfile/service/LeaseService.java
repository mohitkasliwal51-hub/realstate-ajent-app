package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface LeaseService {
    LeaseResponse createLease(LeaseCreateRequest request, SecurityUser currentUser);
    LeaseResponse getLeaseById(UUID id, SecurityUser currentUser);
    LeaseResponse getLeasesForCurrentUser(SecurityUser currentUser);
    List<LeaseResponse> getLeasesByOrganization(SecurityUser currentUser);
    byte[] getLeasePdf(UUID leaseId, SecurityUser currentUser);
    LeaseResponse updateLeaseStatus(UUID leaseId, SecurityUser currentUser, LeaseStatus targetStatus);
    List<LeaseResponse> getExpiringLeases(SecurityUser currentUser, int days);
}
