package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashfreePaymentLinkRequest {
    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;
    @JsonProperty("link_amount")
    private BigDecimal linkAmount;
    @JsonProperty("link_currency")
    private String linkCurrency;
    @JsonProperty("link_id")
    private String linkId;
    @JsonProperty("link_purpose")
    private String linkPurpose;
    @JsonProperty("link_meta")
    private Map<String, String> linkMeta;
    @JsonProperty("link_partial_payments")
    private Boolean linkPartialPayments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetails {
        @JsonProperty("customer_phone")
        private String customerPhone;
        @JsonProperty("customer_email")
        private String customerEmail;
        @JsonProperty("customer_name")
        private String customerName;
    }
}
