package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanVerificationResponse {

    private boolean valid;
    private String panLast4;
    private String registeredName;
    private String nameProvided;
    private String entityType;
    private String nameMatchScore;
    private String nameMatchResult;
    private String aadhaarSeedingStatus;
    private String panStatus;
    private String panStatusCode;
    private String panStatusDescription;
    private String referenceId;
    private String message;
    private boolean linkedToTenant;
    private boolean linkedToLandlord;
}
