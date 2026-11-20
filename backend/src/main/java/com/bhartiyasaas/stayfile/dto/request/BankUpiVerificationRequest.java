package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class BankUpiVerificationRequest {
    @NotBlank(message = "UPI ID / VPA is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{2,256}@[a-zA-Z0-9._-]{2,64}$", message = "Invalid UPI ID format (e.g. user@okhdfcbank)")
    private String vpa;

    @Size(max = 200)
    private String name;

    private UUID landlordId;
}
