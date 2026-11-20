package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeIfscResponse {
    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("reference_id")
    private Object referenceId;

    private String status;
    private String bank;
    private String ifsc;
    private String branch;
    private String address;
    private String city;
    private String state;
    private String micr;
    private String neft;
    private String imps;
    private String rtgs;
    private String upi;

    public boolean isValid() {
        return "VALID".equalsIgnoreCase(status) || (bank != null && !bank.isEmpty());
    }

    public String getReferenceIdAsString() {
        return referenceId == null ? null : String.valueOf(referenceId);
    }
}
