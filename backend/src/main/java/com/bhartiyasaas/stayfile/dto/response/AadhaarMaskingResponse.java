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
public class AadhaarMaskingResponse {

    private boolean valid;
    private String status; // "VALID" or "INVALID_DOCUMENT"
    private String referenceId;
    private String verificationId;
    private String maskedDocumentUrl;
    private String message;
    private boolean linkedToTenant;
    private UUID tenantId;
}
