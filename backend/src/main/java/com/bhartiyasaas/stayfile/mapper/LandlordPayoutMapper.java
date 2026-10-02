package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.LandlordPayoutRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordPayoutResponse;
import com.bhartiyasaas.stayfile.entity.LandlordPayout;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LandlordPayoutMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "landlordId", source = "landlord.id")
    @Mapping(target = "propertyId", source = "property.id")
    LandlordPayoutResponse toResponse(LandlordPayout payout);

    List<LandlordPayoutResponse> toResponseList(List<LandlordPayout> payouts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payoutNumber", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "landlord", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "payoutStatus", ignore = true)
    @Mapping(target = "statementPdfUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LandlordPayout toEntity(LandlordPayoutRequest request);
}
