package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.MaintenanceTicketResponse;
import com.bhartiyasaas.stayfile.entity.enums.TicketStatus;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface MaintenanceTicketService {
    MaintenanceTicketResponse createTicket(MaintenanceTicketCreateRequest request, SecurityUser currentUser);
    List<MaintenanceTicketResponse> getTicketsByOrganization(SecurityUser currentUser);
    List<MaintenanceTicketResponse> getTicketsByTenant(UUID tenantId, SecurityUser currentUser);
    MaintenanceTicketResponse updateTicketStatus(UUID ticketId, SecurityUser currentUser, TicketStatus status);
}
