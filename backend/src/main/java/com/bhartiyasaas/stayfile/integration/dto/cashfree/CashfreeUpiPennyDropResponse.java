package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeUpiPennyDropResponse {
    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("ref_id")
    private Object refId;

    private String status;
    private String vpa;

    @JsonProperty("name_at_bank")
    private String nameAtBank;

    @JsonProperty("bank_account")
    private String bankAccount;

    private String ifsc;

    @JsonProperty("name_match_score")
    private Object nameMatchScore;

    @JsonProperty("name_match_result")
    private String nameMatchResult;

    private String message;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status) || "VALID".equalsIgnoreCase(status);
    }

    public String getRefIdAsString() {
        return refId == null ? null : String.valueOf(refId);
    }
}
