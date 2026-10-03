package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.TicketCategory;
import com.bhartiyasaas.stayfile.entity.enums.TicketPriority;
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
public class MaintenanceTicketCreateRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Builder.Default
    private TicketCategory category = TicketCategory.PLUMBING;

    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;
}
