package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.InvoiceRequest;
import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request, SecurityUser currentUser);
    InvoiceResponse getInvoiceById(UUID id, SecurityUser currentUser);
    List<InvoiceResponse> getInvoicesByLease(UUID leaseId, SecurityUser currentUser);
    List<InvoiceResponse> getInvoicesByOrganization(SecurityUser currentUser);
    InvoiceResponse generateMoveInInvoice(UUID leaseId, SecurityUser currentUser);
    List<InvoiceResponse> generateMonthlyInvoicesForOrganization(SecurityUser currentUser);
    byte[] downloadInvoicePdf(UUID id, SecurityUser currentUser);
    void processOverdueInvoicesScheduled();
}

