package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.PropertyLeadCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyLeadResponse;
import com.bhartiyasaas.stayfile.entity.enums.LeadStatus;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface PropertyLeadService {
    PropertyLeadResponse createLead(PropertyLeadCreateRequest request, SecurityUser currentUser);
    List<PropertyLeadResponse> getLeadsByOrganization(SecurityUser currentUser);
    PropertyLeadResponse updateLeadStatus(UUID leadId, SecurityUser currentUser, LeadStatus status);
}
