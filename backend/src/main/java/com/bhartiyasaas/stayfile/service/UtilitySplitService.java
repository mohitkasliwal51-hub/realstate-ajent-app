package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface UtilitySplitService {

    /**
     * Calculates the PG/Coliving multi-tier electricity split:
     * 1. Common units = Master Discom units - sum(All AC sub-meter units)
     * 2. Per-person common share = Common units / Total residents
     * 3. Per-person room AC share = Room AC units / Room occupants
     * 4. Issues an independent UTILITY_ONLY invoice for each active tenant with transparent audit metadata.
     */
    List<InvoiceResponse> calculateAndGeneratePgUtilityInvoices(
            UUID propertyId,
            LocalDate billingMonth,
            SecurityUser currentUser);

    /**
     * Automated job that checks properties where a master Discom bill is present,
     * sub-meters are logged, and auto-generates utility invoices per billing cycle.
     */
    List<InvoiceResponse> autoProcessDueUtilitySplits();
}
