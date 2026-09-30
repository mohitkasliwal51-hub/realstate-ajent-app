package com.bhartiyasaas.stayfile.security;

public interface LeaseSigningProvider {
    SigningResult requestEStamp(String leaseId);
    SigningResult requestAadhaarEsign(String leaseId);

    record SigningResult(String transactionId, String documentNumber, String message) {
    }
}