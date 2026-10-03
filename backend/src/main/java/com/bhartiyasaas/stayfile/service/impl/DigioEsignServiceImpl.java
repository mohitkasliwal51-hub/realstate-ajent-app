package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.service.EsignProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DigioEsignServiceImpl implements EsignProvider {

    @Value("${stayfile.kyc.digio.client-id:}")
    private String clientId;

    @Value("${stayfile.kyc.digio.client-secret:}")
    private String clientSecret;

    @Override
    public String initiateLeaseEsign(UUID leaseId, String stateCode, int stampAmount, String landlordPhone, String tenantPhone) {
        if (clientId == null || clientId.trim().isEmpty() || clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new ProviderUnavailableException("Digio e-Stamping & eSign API credentials are not configured.");
        }
        throw new ProviderUnavailableException("Digio e-Stamping integration requires live client credentials.");
    }
}
