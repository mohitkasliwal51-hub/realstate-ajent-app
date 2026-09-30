package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.LeadSource;
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
public class PropertyLeadCreateRequest {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    private UUID propertyId;
    private UUID unitId;

    @NotBlank(message = "Lead name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String email;

    @Builder.Default
    private LeadSource source = LeadSource.WEBSITE;

    private String notes;
}
