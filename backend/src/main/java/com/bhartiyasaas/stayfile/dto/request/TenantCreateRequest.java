package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCreateRequest {

    private UUID userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Permanent address is required")
    private String permanentAddress;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    private String idProofType;
    private String idProofNumber;
    private String idProofDocumentUrl;
}
