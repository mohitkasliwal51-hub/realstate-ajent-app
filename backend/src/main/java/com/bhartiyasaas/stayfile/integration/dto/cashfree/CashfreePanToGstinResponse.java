package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreePanToGstinResponse {
    @JsonProperty("reference_id") private Object referenceId;
    @JsonProperty("verification_id")
    private String verificationId;
    private String status;
    private String pan;
    @JsonProperty("gstin_list")
    @JsonAlias("gstinList")
    private List<GstinRecord> gstins;
    public String getReferenceIdAsString() { return referenceId == null ? null : String.valueOf(referenceId); }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GstinRecord {
        private String gstin;
        private String status;
        private String state;
    }
}
