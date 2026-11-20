package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeRpdCreateResponse {
    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("ref_id")
    private Object refId;

    private String status;
    private String message;

    @JsonProperty("valid_upto")
    private String validUpto;

    @JsonProperty("upi_link")
    private String upiLink;

    private String gpay;
    private String bhim;
    private String paytm;
    private String phonepe;

    @JsonProperty("qr_code")
    private String qrCode;

    private String url;

    public String getRefIdAsString() {
        return refId == null ? null : String.valueOf(refId);
    }

    public Map<String, String> getPspLinks() {
        Map<String, String> links = new java.util.LinkedHashMap<>();
        if (gpay != null) links.put("gpay", gpay);
        if (bhim != null) links.put("bhim", bhim);
        if (paytm != null) links.put("paytm", paytm);
        if (phonepe != null) links.put("phonepe", phonepe);
        return links;
    }
}
