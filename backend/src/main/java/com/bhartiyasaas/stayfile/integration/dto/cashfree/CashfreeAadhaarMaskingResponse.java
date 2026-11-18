package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeAadhaarMaskingResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("image_link")
    private String imageLink;

    public String getReferenceIdAsString() {
        return referenceId != null ? String.valueOf(referenceId) : null;
    }
}
