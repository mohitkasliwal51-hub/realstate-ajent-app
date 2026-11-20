package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CinVerificationResponse {
    private boolean valid;
    private String cin;
    private String verificationId;
    private String companyName;
    private String registrationNumber;
    private String incorporationDate;
    private String companyStatus;
    private String email;
    private String incorporationCountry;
    private List<Object> directors;
    private String referenceId;
    private boolean linkedToLandlord;
}
