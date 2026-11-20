package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BankVerificationResultResponse {
    private boolean valid;
    private String verificationMode; // REVERSE_PENNY_DROP, BAV_SYNC, UPI_PENNY_DROP
    private String accountNumber;
    private String ifsc;
    private String bankName;
    private String branch;
    private String accountHolderName;
    private String upiId;
    private String nameMatchScore;
    private String nameMatchResult;
    private String utr;
    private String referenceId;
    private String message;
    private boolean linkedToLandlord;
    private Map<String, Object> metadata;
}
