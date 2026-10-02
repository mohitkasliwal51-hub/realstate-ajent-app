package com.bhartiyasaas.stayfile.mapper;

import com.bhartiyasaas.stayfile.dto.request.InvoiceRequest;
import com.bhartiyasaas.stayfile.dto.response.InvoiceLineItemResponse;
import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.InvoiceLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "leaseId", source = "lease.id")
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "unitId", source = "unit.id")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    @Mapping(target = "invoiceId", source = "invoice.id")
    InvoiceLineItemResponse toLineItemResponse(InvoiceLineItem lineItem);

    List<InvoiceLineItemResponse> toLineItemResponseList(List<InvoiceLineItem> lineItems);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "lease", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "balanceDue", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "invoicePdfUrl", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "lineItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Invoice toEntity(InvoiceRequest request);
}
