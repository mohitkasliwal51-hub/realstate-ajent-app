package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.PropertyLeadCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyLeadResponse;
import com.bhartiyasaas.stayfile.entity.enums.LeadStatus;

import java.util.List;
import java.util.UUID;

public interface PropertyLeadService {
    PropertyLeadResponse createLead(PropertyLeadCreateRequest request);
    List<PropertyLeadResponse> getLeadsByOrganization(UUID organizationId);
    PropertyLeadResponse updateLeadStatus(UUID leadId, UUID organizationId, LeadStatus status);
}
