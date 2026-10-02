package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.MeterReadingRequest;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.entity.MeterReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MeterReadingMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "unitId", source = "unit.id")
    MeterReadingResponse toResponse(MeterReading reading);

    List<MeterReadingResponse> toResponseList(List<MeterReading> readings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "unitsConsumed", ignore = true)
    @Mapping(target = "totalCharge", ignore = true)
    @Mapping(target = "isBilled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MeterReading toEntity(MeterReadingRequest request);
}
