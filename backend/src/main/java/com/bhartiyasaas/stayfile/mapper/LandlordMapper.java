package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.LandlordRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LandlordMapper {

    @Mapping(target = "managingOrganizationId", source = "managingOrganization.id")
    @Mapping(target = "profileId", source = "profile.id")
    LandlordResponse toResponse(Landlord landlord);

    List<LandlordResponse> toResponseList(List<Landlord> landlords);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "managingOrganization", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Landlord toEntity(LandlordRequest request);
}
