package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class GstinVerificationRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z]Z[0-9A-Za-z]$")
    private String gstin;
    @Size(max = 200)
    private String businessName;
    private UUID landlordId;
}
