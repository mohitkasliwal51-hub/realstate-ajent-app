package com.bhartiyasaas.stayfile.security;

import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class KycProviderPlaceholder implements KycVerificationProvider {

    @Override
    public VerificationResult verifyAadhaar(String aadhaarNumber) {
        return notConfigured("Aadhaar", aadhaarNumber);
    }

    @Override
    public VerificationResult verifyPan(String panNumber) {
        return notConfigured("PAN", panNumber);
    }

    private VerificationResult notConfigured(String documentType, String documentNumber) {
        return new VerificationResult(
                VerificationStatus.PENDING,
                null,
                "TODO: Configure Surepass or Digio credentials before verifying " + documentType
        );
    }
}