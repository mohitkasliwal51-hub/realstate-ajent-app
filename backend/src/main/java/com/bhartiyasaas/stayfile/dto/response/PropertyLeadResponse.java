package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.LeadSource;
import com.bhartiyasaas.stayfile.entity.enums.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyLeadResponse {

    private UUID id;
    private UUID organizationId;
    private UUID propertyId;
    private String propertyName;
    private UUID unitId;
    private UUID assignedToId;
    private String name;
    private String phone;
    private String email;
    private LeadSource source;
    private LeadStatus status;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
