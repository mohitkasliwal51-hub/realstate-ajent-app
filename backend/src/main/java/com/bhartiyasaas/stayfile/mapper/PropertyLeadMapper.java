package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.PropertyLeadCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyLeadResponse;
import com.bhartiyasaas.stayfile.entity.PropertyLead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PropertyLeadMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "propertyId", source = "property.id")
    @Mapping(target = "propertyName", source = "property.name")
    @Mapping(target = "unitId", source = "unit.id")
    @Mapping(target = "assignedToId", ignore = true)
    PropertyLeadResponse toResponse(PropertyLead lead);

    List<PropertyLeadResponse> toResponseList(List<PropertyLead> leads);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PropertyLead toEntity(PropertyLeadCreateRequest request);
}
