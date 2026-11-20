package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankIfscLookupResponse {
    private boolean valid;
    private String ifsc;
    private String bank;
    private String branch;
    private String address;
    private String city;
    private String state;
    private String micr;
    private String imps;
    private String rtgs;
    private String neft;
    private String upi;
    private String message;
}
