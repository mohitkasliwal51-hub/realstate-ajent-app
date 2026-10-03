package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.PropertyCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyResponse;
import com.bhartiyasaas.stayfile.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PropertyMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "landlordId", source = "landlord.id")
    @Mapping(target = "landlordName", source = "landlord.legalName")
    PropertyResponse toResponse(Property property);

    List<PropertyResponse> toResponseList(List<Property> properties);

    @Mapping(target = "organizationId", source = "organization.id")
    com.bhartiyasaas.stayfile.dto.response.PublicPropertyResponse toPublicResponse(Property property);

    List<com.bhartiyasaas.stayfile.dto.response.PublicPropertyResponse> toPublicResponseList(List<Property> properties);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "landlord", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Property toEntity(PropertyCreateRequest request);
}
