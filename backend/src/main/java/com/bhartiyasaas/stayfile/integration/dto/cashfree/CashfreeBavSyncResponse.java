package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeBavSyncResponse {
    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("account_status")
    private String accountStatus;

    @JsonProperty("account_status_code")
    private String accountStatusCode;

    @JsonProperty("name_at_bank")
    private String nameAtBank;

    @JsonProperty("bank_name")
    private String bankName;

    private String branch;
    private String city;
    private String micr;

    @JsonProperty("name_match_score")
    private String nameMatchScore;

    @JsonProperty("name_match_result")
    private String nameMatchResult;

    private String utr;
    private String message;

    public boolean isValid() {
        return "VALID".equalsIgnoreCase(accountStatus) || "ACCOUNT_IS_VALID".equalsIgnoreCase(accountStatusCode);
    }

    public String getReferenceIdAsString() {
        return referenceId == null ? null : String.valueOf(referenceId);
    }
}
