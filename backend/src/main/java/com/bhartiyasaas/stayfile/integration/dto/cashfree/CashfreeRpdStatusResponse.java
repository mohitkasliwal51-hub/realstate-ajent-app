package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeRpdStatusResponse {
    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("ref_id")
    private Object refId;

    private String status;

    @JsonProperty("bank_account")
    private String bankAccount;

    private String ifsc;

    @JsonProperty("name_at_bank")
    private String nameAtBank;

    @JsonProperty("upi")
    @JsonAlias("upi_id")
    private String upiId;

    private String amount;

    @JsonProperty("reversal_status")
    @JsonAlias("refund_status")
    private String refundStatus;

    private String utr;

    @JsonProperty("name_match_score")
    private String nameMatchScore;

    @JsonProperty("name_match_result")
    private String nameMatchResult;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public String getRefIdAsString() {
        return refId == null ? null : String.valueOf(refId);
    }
}
