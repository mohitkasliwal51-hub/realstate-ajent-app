package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.MaintenanceTicketResponse;

import java.util.List;
import java.util.UUID;

public interface MaintenanceTicketService {
    MaintenanceTicketResponse createTicket(MaintenanceTicketCreateRequest request);
    List<MaintenanceTicketResponse> getTicketsByOrganization(UUID organizationId);
    List<MaintenanceTicketResponse> getTicketsByTenant(UUID tenantId, UUID organizationId);
    MaintenanceTicketResponse updateTicketStatus(UUID ticketId, UUID organizationId, String status);
}
