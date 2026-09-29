package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.PropertyMapper;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.service.PropertyService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final ProfileRepository profileRepository;
    private final PropertyMapper propertyMapper;

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        if (owner.getOrganization() != null && !owner.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Owner profile does not belong to the specified Organization");
        }

        Property property = propertyMapper.toEntity(request);
        property.setOrganization(organization);
        property.setOwner(owner);
        property.setIsActive(true);

        Property savedProperty = propertyRepository.save(property);
        return propertyMapper.toResponse(savedProperty);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(UUID id, UUID organizationId) {
        Property property = propertyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + id));
        return propertyMapper.toResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByOrganization(UUID organizationId) {
        return propertyMapper.toResponseList(propertyRepository.findByOrganizationId(organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPublicShowcaseProperties() {
        return propertyRepository.findAll().stream()
                .filter(property -> Boolean.TRUE.equals(property.getIsActive()))
                .map(propertyMapper::toResponse)
                .collect(Collectors.toList());
    }
}
