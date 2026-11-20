package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class BankSyncVerificationRequest {
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{6,40}$", message = "Bank account number must contain only digits")
    private String bankAccount;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "Invalid IFSC format")
    private String ifsc;

    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{0,99}$", message = "Invalid account holder name")
    private String accountHolderName;

    @Pattern(regexp = "^[0-9]{8,13}$", message = "Phone number must contain 8 to 13 digits")
    private String phone;

    private UUID landlordId;
}
