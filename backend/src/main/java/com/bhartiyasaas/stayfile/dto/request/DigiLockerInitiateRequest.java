package com.bhartiyasaas.stayfile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerInitiateRequest {

    private UUID tenantId;

    @Pattern(regexp = "^https://[^\\s]+$", message = "Redirect URL must use HTTPS")
    private String redirectUrl;

    @Pattern(regexp = "signup|login|kyc", message = "User flow must be signup, login, or kyc")
    @Builder.Default
    private String userFlow = "signup";
}
