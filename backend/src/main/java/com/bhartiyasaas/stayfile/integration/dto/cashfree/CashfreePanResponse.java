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
public class CashfreePanResponse {

    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("registered_name")
    private String registeredName;

    @JsonProperty("name_provided")
    private String nameProvided;

    @JsonProperty("type")
    private String type;

    @JsonProperty("valid")
    private Boolean valid;

    @JsonProperty("message")
    private String message;

    @JsonProperty("name_match_score")
    private Object nameMatchScore;

    @JsonProperty("name_match_result")
    private String nameMatchResult;

    @JsonProperty("aadhaar_seeding_status")
    private String aadhaarSeedingStatus;

    @JsonProperty("pan_status")
    private String panStatus;

    @JsonProperty("pan_status_code")
    private String panStatusCode;

    @JsonProperty("pan_status_desc")
    private String panStatusDesc;

    public String getReferenceIdAsString() {
        return referenceId != null ? String.valueOf(referenceId) : null;
    }

    public String getNameMatchScoreAsString() {
        return nameMatchScore != null ? String.valueOf(nameMatchScore) : null;
    }
}
