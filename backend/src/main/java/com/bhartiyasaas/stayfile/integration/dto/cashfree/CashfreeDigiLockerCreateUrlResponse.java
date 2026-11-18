package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeDigiLockerCreateUrlResponse {

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("url")
    private String url;

    @JsonProperty("status")
    private String status;

    @JsonProperty("document_requested")
    private List<String> documentRequested;

    @JsonProperty("redirect_url")
    private String redirectUrl;

    @JsonProperty("user_flow")
    private String userFlow;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("company_name")
    private String companyName;

    public String getReferenceIdAsString() {
        return referenceId != null ? String.valueOf(referenceId) : null;
    }
}
