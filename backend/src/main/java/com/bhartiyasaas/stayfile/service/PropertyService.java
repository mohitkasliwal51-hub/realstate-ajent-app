package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface PropertyService {
    PropertyResponse createProperty(PropertyCreateRequest request, SecurityUser currentUser);
    PropertyResponse updateProperty(UUID id, PropertyCreateRequest request, SecurityUser currentUser);
    PropertyResponse togglePropertyActive(UUID id, Boolean isActive, SecurityUser currentUser);
    PropertyResponse getPropertyById(UUID id, SecurityUser currentUser);
    List<PropertyResponse> getPropertiesByOrganization(SecurityUser currentUser);
    List<PropertyResponse> getPublicPropertiesByOrganization(UUID organizationId);
    List<PropertyResponse> getPublicShowcaseProperties();
}
