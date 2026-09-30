package com.bhartiyasaas.stayfile.security;

import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;

public interface KycVerificationProvider {
    VerificationResult verifyAadhaar(String aadhaarNumber);
    VerificationResult verifyPan(String panNumber);

    record VerificationResult(VerificationStatus status, String providerReference, String message) {
    }
}