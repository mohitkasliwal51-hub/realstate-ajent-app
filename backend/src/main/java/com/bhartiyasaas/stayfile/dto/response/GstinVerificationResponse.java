package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GstinVerificationResponse {
    private boolean valid;
    private String gstin;
    private String legalName;
    private String tradeName;
    private String registrationDate;
    private String constitution;
    private String taxpayerType;
    private String status;
    private String centerJurisdiction;
    private String stateJurisdiction;
    private String lastUpdateDate;
    private String address;
    private java.util.List<String> activities;
    private java.util.List<java.util.Map<String, Object>> additionalAddresses;
    private Map<String, Object> metadata;
    private String referenceId;
    private String message;
    private boolean linkedToLandlord;
}
