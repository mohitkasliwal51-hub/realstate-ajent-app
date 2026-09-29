package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;

public interface PropertyService {
    PropertyResponse createProperty(PropertyCreateRequest request);
    PropertyResponse getPropertyById(UUID id, UUID organizationId);
    List<PropertyResponse> getPropertiesByOrganization(UUID organizationId);
    List<PropertyResponse> getPublicShowcaseProperties();
}
