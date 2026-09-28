package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCreateRequest {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String alternatePhone;
    private String gender;
    private LocalDate dateOfBirth;

    @NotBlank(message = "Permanent address is required")
    private String permanentAddress;

    private String occupation;
    private String organizationOrCollege;
    private String workAddress;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    private String idProofType;
    private String idProofNumber;
    private String idProofFrontUrl;
    private String idProofBackUrl;
    private String tenantPhotoUrl;
}
