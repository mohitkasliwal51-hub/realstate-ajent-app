package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.UnitCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.UnitResponse;
import com.bhartiyasaas.stayfile.entity.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UnitMapper {

    @Mapping(target = "propertyId", source = "property.id")
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "currentLeaseId", source = "currentLease.id")
    UnitResponse toResponse(Unit unit);

    List<UnitResponse> toResponseList(List<Unit> units);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "currentLease", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Unit toEntity(UnitCreateRequest request);
}
