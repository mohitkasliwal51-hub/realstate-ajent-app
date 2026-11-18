package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanVerificationRequest {

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[a-zA-Z]{5}[0-9]{4}[a-zA-Z]{1}$", message = "Invalid PAN format. Must be 10 alphanumeric characters (e.g. ABCDE1234F)")
    private String pan;

    private String name;

    private UUID tenantId;

    private UUID landlordId;
}
