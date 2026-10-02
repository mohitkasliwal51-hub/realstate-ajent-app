package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.OwnerType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordResponse {

    private UUID id;
    private UUID managingOrganizationId;
    private UUID profileId;
    private OwnerType ownerType;
    private String legalName;
    private String email;
    private String phone;
    private String pan;
    private String gstin;
    private String address;

    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String accountHolderName;
    private String ownerUpiId;

    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
