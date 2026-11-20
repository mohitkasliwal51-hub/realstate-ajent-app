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
public class CashfreeRpdCreateRequest {
    @JsonProperty("verification_id")
    private String verificationId;
    private String name;

    @JsonProperty("redirect_url")
    private String redirectUrl;
}
