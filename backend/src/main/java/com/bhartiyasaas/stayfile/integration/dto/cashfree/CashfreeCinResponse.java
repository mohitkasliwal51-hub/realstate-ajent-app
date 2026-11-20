package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeCinResponse {
    @JsonProperty("verification_id") private String verificationId;
    private String status;
    private String cin;
    @JsonProperty("company_name") private String companyName;
    @JsonProperty("registration_number") private String registrationNumber;
    @JsonProperty("incorporation_date") private String incorporationDate;
    @JsonProperty("cin_status") private String companyStatus;
    private String email;
    @JsonProperty("incorporation_country") private String incorporationCountry;
    @JsonProperty("director_details") private List<Object> directors;
    @JsonProperty("reference_id") private Object referenceId;
    public String getReferenceIdAsString() { return referenceId == null ? null : String.valueOf(referenceId); }
    public boolean isValid() { return "VALID".equalsIgnoreCase(status); }
}
