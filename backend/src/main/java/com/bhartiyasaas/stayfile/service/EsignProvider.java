package com.bhartiyasaas.stayfile.service;

import java.util.UUID;

public interface EsignProvider {
    /**
     * Initiates Digio e-Stamping and Aadhaar eSign workflow for a lease agreement.
     */
    String initiateLeaseEsign(UUID leaseId, String stateCode, int stampAmount, String landlordPhone, String tenantPhone);
}
