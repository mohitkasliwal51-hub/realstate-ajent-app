package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreeDigiLockerAadhaarDocumentResponse {

    @JsonProperty("status")
    private String status; // "SUCCESS", "VALID", "AADHAAR_NOT_LINKED", "INVALID"

    @JsonProperty("reference_id")
    private Object referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("care_of")
    private String careOf;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("year_of_birth")
    private Object yearOfBirth;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("name")
    private String name;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("message")
    private String message;

    @JsonProperty("code")
    private String code;

    @JsonProperty("split_address")
    private Map<String, Object> splitAddress;

    @JsonProperty("image")
    @JsonAlias({"photo_link", "image"})
    private String image;

    @JsonProperty("xml_file")
    private String xmlFile;

    public String getReferenceIdAsString() {
        return referenceId != null ? String.valueOf(referenceId) : null;
    }

    public String getUidLast4() {
        if (uid != null && uid.length() >= 4) {
            return uid.substring(uid.length() - 4);
        }
        return null;
    }

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(status) || "VALID".equalsIgnoreCase(status);
    }

    public boolean isValidationPending() {
        return "VALIDATION_PENDING".equalsIgnoreCase(code)
                || "PENDING".equalsIgnoreCase(status);
    }

    public String getFormattedAddress() {
        if (splitAddress == null || splitAddress.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, splitAddress.get("address_line"));
        appendIfPresent(sb, splitAddress.get("house"));
        appendIfPresent(sb, splitAddress.get("street"));
        appendIfPresent(sb, splitAddress.get("landmark"));
        appendIfPresent(sb, splitAddress.get("vtc"));
        appendIfPresent(sb, splitAddress.get("po"));
        appendIfPresent(sb, splitAddress.get("subdist"));
        Object dist = splitAddress.get("dist") != null ? splitAddress.get("dist") : splitAddress.get("district");
        appendIfPresent(sb, dist);
        appendIfPresent(sb, splitAddress.get("state"));
        appendIfPresent(sb, splitAddress.get("country"));
        Object pin = splitAddress.get("pincode");
        if (pin != null && !String.valueOf(pin).isBlank()) {
            sb.append(" - ").append(pin);
        }
        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, Object val) {
        if (val != null) {
            String s = String.valueOf(val).trim();
            if (!s.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(s);
            }
        }
    }
}
