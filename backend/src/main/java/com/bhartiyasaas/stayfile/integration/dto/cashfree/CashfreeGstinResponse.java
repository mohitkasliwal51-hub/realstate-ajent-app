package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeGstinResponse {
    private Boolean valid;
    @JsonProperty("GSTIN")
    @JsonAlias("gstin")
    private String gstin;
    @JsonProperty("legal_name_of_business") private String legalName;
    @JsonProperty("trade_name_of_business") private String tradeName;
    @JsonProperty("date_of_registration") private String registrationDate;
    @JsonProperty("constitution_of_business") private String constitution;
    @JsonProperty("taxpayer_type") private String taxpayerType;
    @JsonProperty("gst_in_status") private String status;
    @JsonProperty("center_jurisdiction") private String centerJurisdiction;
    @JsonProperty("state_jurisdiction") private String stateJurisdiction;
    @JsonProperty("last_update_date") private String lastUpdateDate;
    @JsonProperty("principal_place_address") private String address;
    @JsonProperty("principal_place_split_address") private Map<String, Object> splitAddress;
    @JsonProperty("nature_of_business_activities") private List<String> activities;
    @JsonProperty("additional_address_array") private List<Map<String, Object>> additionalAddresses;
    @JsonProperty("reference_id") private Object referenceId;
    private String message;
    public String getReferenceIdAsString() { return referenceId == null ? null : String.valueOf(referenceId); }

    public boolean isValid() {
        return Boolean.TRUE.equals(valid)
                || "ACTIVE".equalsIgnoreCase(status)
                || (gstin != null && !gstin.isBlank() && (message == null || !message.toLowerCase().contains("invalid")));
    }
}
