package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreeUpiPennyDropRequest {
    @JsonProperty("verification_id")
    private String verificationId;

    private String vpa;
    private String name;

    @JsonProperty("user_consent")
    private UserConsent userConsent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserConsent {
        private Boolean obtained;
        private String type;
        private String timestamp;
        private String purpose;
    }
}
