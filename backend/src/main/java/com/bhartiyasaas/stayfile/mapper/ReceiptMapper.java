package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.entity.Receipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReceiptMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "leaseId", source = "lease.id")
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "tenantName", source = "tenant.fullName")
    @Mapping(target = "ownerId", source = "owner.id")
    ReceiptResponse toResponse(Receipt receipt);

    List<ReceiptResponse> toResponseList(List<Receipt> receipts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receiptNumber", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "lease", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "receiptPdfUrl", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Receipt toEntity(ReceiptCreateRequest request);
}
