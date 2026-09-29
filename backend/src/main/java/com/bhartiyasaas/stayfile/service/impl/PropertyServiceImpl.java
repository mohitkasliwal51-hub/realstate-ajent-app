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

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        Profile owner = profileRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found with ID: " + request.getOwnerId()));

        Property property = Property.builder()
                .organization(organization)
                .owner(owner)
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .amenities(request.getAmenities())
                .rules(request.getRules())
                .images(request.getImages())
                .description(request.getDescription())
                .isActive(true)
                .build();

        Property savedProperty = propertyRepository.save(property);
        return mapToResponse(savedProperty);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(UUID id, UUID organizationId) {
        Property property = propertyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + id));
        return mapToResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByOrganization(UUID organizationId) {
        return propertyRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPublicShowcaseProperties() {
        return propertyRepository.findAll().stream()
                .filter(property -> Boolean.TRUE.equals(property.getIsActive()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PropertyResponse mapToResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .organizationId(property.getOrganization().getId())
                .ownerId(property.getOwner().getId())
                .name(property.getName())
                .type(property.getType())
                .address(property.getAddress())
                .city(property.getCity())
                .state(property.getState())
                .pincode(property.getPincode())
                .landmark(property.getLandmark())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .amenities(property.getAmenities())
                .rules(property.getRules())
                .images(property.getImages())
                .description(property.getDescription())
                .isActive(property.getIsActive())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}
