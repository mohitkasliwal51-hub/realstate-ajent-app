package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeDigiLockerStatusResponse {

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("user_details")
    private Map<String, Object> userDetails;

    @JsonProperty("document_requested")
    private List<String> documentRequested;

    @JsonProperty("document_consent")
    private List<String> documentConsent;

    @JsonProperty("document_consent_validity")
    private String documentConsentValidity;

    public String getReferenceIdAsString() {
        return referenceId != null ? String.valueOf(referenceId) : null;
    }
}
