package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.LeaseCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.LeaseResponse;
import com.bhartiyasaas.stayfile.entity.Lease;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaseMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "unitId", source = "unit.id")
    @Mapping(target = "unitNumber", source = "unit.unitNumber")
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "tenantName", source = "tenant.fullName")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.fullName")
    @Mapping(target = "eStampNumber", source = "EStampNumber")
    LeaseResponse toResponse(Lease lease);

    List<LeaseResponse> toResponseList(List<Lease> leases);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "agreementTemplate", ignore = true)
    @Mapping(target = "agreementPdfUrl", ignore = true)
    @Mapping(target = "isEsignCompleted", ignore = true)
    @Mapping(target = "esignTransactionId", ignore = true)
    @Mapping(target = "eStampNumber", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lease toEntity(LeaseCreateRequest request);
}
