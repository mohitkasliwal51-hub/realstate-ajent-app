package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CinVerificationRequest {
    @NotBlank
    @Pattern(regexp = "^[ULul][0-9]{5}[A-Za-z]{2}[0-9]{4}[A-Za-z]{3}[0-9]{6}$")
    private String cin;
    private UUID landlordId;
}
