package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {

    private UUID id;
    private UUID organizationId;
    private UUID ownerId;
    private String fullName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String gender;
    private LocalDate dateOfBirth;
    private String permanentAddress;
    private String occupation;
    private String organizationOrCollege;
    private String workAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String idProofType;
    private String idProofLast4;
    private Boolean isIdVerified;
    private String idProofFrontUrl;
    private String idProofBackUrl;
    private String tenantPhotoUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
