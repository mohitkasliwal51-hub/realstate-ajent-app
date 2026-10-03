package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.PropertyMapper;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.PropertyService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final LandlordRepository landlordRepository;
    private final PropertyMapper propertyMapper;

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Landlord landlord = null;
        if (request.getLandlordId() != null) {
            landlord = landlordRepository.findByIdAndManagingOrganizationId(request.getLandlordId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + request.getLandlordId()));
        }

        Property property = propertyMapper.toEntity(request);
        property.setOrganization(organization);
        property.setLandlord(landlord);
        property.setIsActive(true);

        Property savedProperty = propertyRepository.save(property);
        return propertyMapper.toResponse(savedProperty);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Property property = propertyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + id));
        return propertyMapper.toResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        return propertyMapper.toResponseList(propertyRepository.findByOrganizationId(organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPublicPropertiesByOrganization(UUID organizationId) {
        return propertyMapper.toResponseList(propertyRepository.findByOrganizationIdAndIsActiveTrue(organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPublicShowcaseProperties() {
        return propertyMapper.toResponseList(propertyRepository.findByIsActiveTrue());
    }
}
