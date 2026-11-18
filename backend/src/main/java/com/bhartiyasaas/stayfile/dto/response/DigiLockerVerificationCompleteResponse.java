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
public class DigiLockerVerificationCompleteResponse {

    private boolean verified;
    private String status;
    private UUID tenantId;
    private String legalName;
    private String maskedAadhaar;
    private String dob;
    private String gender;
    private String careOf;
    private String permanentAddress;
    private String photoDocumentUrl;
    private String verificationId;
    private String referenceId;
    private String message;
}
