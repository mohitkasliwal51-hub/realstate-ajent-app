package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerInitiateResponse {

    private String verificationId;
    private String referenceId;
    private String consentUrl;
    private String status;
    private int expiresInMinutes;
    private UUID tenantId;
    private String message;
}
