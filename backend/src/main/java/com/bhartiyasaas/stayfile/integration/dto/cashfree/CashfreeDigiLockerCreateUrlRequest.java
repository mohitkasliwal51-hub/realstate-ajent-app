package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashfreeDigiLockerCreateUrlRequest {

    @JsonProperty("verification_id")
    private String verificationId;

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
}
