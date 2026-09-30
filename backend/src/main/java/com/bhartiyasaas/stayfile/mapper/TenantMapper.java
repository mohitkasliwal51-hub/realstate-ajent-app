package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.TenantCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.TenantResponse;
import com.bhartiyasaas.stayfile.entity.Tenant;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TenantMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "userId", source = "user.id")
    TenantResponse toResponse(Tenant tenant);

    List<TenantResponse> toResponseList(List<Tenant> tenants);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "idProofLast4", ignore = true)
    @Mapping(target = "idProofNumber", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tenant toEntity(TenantCreateRequest request);
}
