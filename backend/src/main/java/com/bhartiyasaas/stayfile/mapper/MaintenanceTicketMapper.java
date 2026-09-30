package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.MaintenanceTicketResponse;
import com.bhartiyasaas.stayfile.entity.MaintenanceTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MaintenanceTicketMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "unitId", source = "unit.id")
    @Mapping(target = "unitNumber", source = "unit.unitNumber")
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "tenantName", source = "tenant.fullName")
    MaintenanceTicketResponse toResponse(MaintenanceTicket ticket);

    List<MaintenanceTicketResponse> toResponseList(List<MaintenanceTicket> tickets);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MaintenanceTicket toEntity(MaintenanceTicketCreateRequest request);
}
