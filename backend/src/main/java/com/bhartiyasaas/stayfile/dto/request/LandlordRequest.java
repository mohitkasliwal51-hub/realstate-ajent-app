package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.OwnerType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordRequest {

    private UUID profileId;

    @Builder.Default
    private OwnerType ownerType = OwnerType.INDIVIDUAL;

    @NotBlank(message = "Legal name is required")
    private String legalName;

    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String pan;
    private String gstin;
    private String address;

    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String accountHolderName;
    private String ownerUpiId;
}
