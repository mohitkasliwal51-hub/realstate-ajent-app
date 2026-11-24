package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashfreePaymentLinkResponse {
    @JsonProperty("cf_link_id")
    private String cfLinkId;
    @JsonProperty("link_id")
    private String linkId;
    @JsonProperty("link_status")
    private String linkStatus;
    @JsonProperty("link_amount")
    private BigDecimal linkAmount;
    @JsonProperty("link_url")
    private String linkUrl;
    @JsonProperty("link_expiry_time")
    private String linkExpiryTime;
}
