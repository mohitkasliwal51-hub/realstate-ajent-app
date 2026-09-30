package com.bhartiyasaas.stayfile.security;

import org.springframework.stereotype.Component;

@Component
public class LeaseSigningProviderPlaceholder implements LeaseSigningProvider {

    @Override
    public SigningResult requestEStamp(String leaseId) {
        return notConfigured("e-stamp");
    }

    @Override
    public SigningResult requestAadhaarEsign(String leaseId) {
        return notConfigured("Aadhaar OTP e-sign");
    }

    private SigningResult notConfigured(String operation) {
        return new SigningResult(null, null, "TODO: Configure a provider before requesting " + operation);
    }
}