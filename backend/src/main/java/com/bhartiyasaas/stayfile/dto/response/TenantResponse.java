package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String permanentAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String idProofType;
    private String idProofLast4;
    private VerificationStatus kycStatus;
    private String idProofDocumentUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
