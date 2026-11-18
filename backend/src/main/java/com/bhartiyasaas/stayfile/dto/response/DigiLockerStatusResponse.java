package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerStatusResponse {

    private String verificationId;
    private String referenceId;
    private String status; // PENDING, AUTHENTICATED, EXPIRED, CONSENT_DENIED
    private String message;
    private boolean authenticated;
    private boolean canComplete;
    private boolean expired;
    private boolean consentDenied;
    private String documentConsentValidity;
    private String actionRequired; // WAIT_FOR_USER_OTP, PROCEED_TO_COMPLETE, REINITIATE_SESSION
}
