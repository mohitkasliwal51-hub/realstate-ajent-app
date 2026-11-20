package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BankRpdInitiateResponse {
    private String verificationId;
    private String refId;
    private String status;
    private String message;
    private String upiIntentUrl;
    private String qrCode;
    private Map<String, String> pspLinks;
    private String validUpto;
    private String url;
}
