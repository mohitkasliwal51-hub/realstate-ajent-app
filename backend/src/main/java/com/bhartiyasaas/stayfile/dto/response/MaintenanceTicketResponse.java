package com.bhartiyasaas.stayfile.dto.response;

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
public class MaintenanceTicketResponse {

    private UUID id;
    private UUID organizationId;
    private UUID unitId;
    private String unitNumber;
    private UUID tenantId;
    private String tenantName;
    private UUID assignedToId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
