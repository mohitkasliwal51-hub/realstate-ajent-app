package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.MaintenanceTicketResponse;
import com.bhartiyasaas.stayfile.entity.MaintenanceTicket;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.TicketCategory;
import com.bhartiyasaas.stayfile.entity.enums.TicketPriority;
import com.bhartiyasaas.stayfile.entity.enums.TicketStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.MaintenanceTicketMapper;
import com.bhartiyasaas.stayfile.repository.MaintenanceTicketRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.MaintenanceTicketService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceTicketServiceImpl implements MaintenanceTicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final MaintenanceTicketMapper ticketMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public MaintenanceTicketResponse createTicket(MaintenanceTicketCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Unit unit = unitRepository.findByIdAndOrganizationId(request.getUnitId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        Tenant tenant = tenantRepository.findByIdAndOrganizationId(request.getTenantId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        tenantAccessService.validateTenantOwnership(tenant, "maintenance ticket");

        MaintenanceTicket ticket = ticketMapper.toEntity(request);
        ticket.setOrganization(organization);
        ticket.setUnit(unit);
        ticket.setTenant(tenant);
        ticket.setStatus(TicketStatus.OPEN);
        if (ticket.getCategory() == null) ticket.setCategory(TicketCategory.PLUMBING);
        if (ticket.getPriority() == null) ticket.setPriority(TicketPriority.MEDIUM);

        MaintenanceTicket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicketResponse> getTicketsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        List<MaintenanceTicket> tickets = ticketRepository.findByOrganizationId(organizationId);
        return ticketMapper.toResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicketResponse> getTicketsByTenant(UUID tenantId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Tenant tenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        tenantAccessService.validateTenantOwnership(tenant, "maintenance tickets");
        List<MaintenanceTicket> tickets = ticketRepository.findByTenantIdAndOrganizationId(tenantId, organizationId);
        return ticketMapper.toResponseList(tickets);
    }

    @Override
    @Transactional
    public MaintenanceTicketResponse updateTicketStatus(UUID ticketId, SecurityUser currentUser, TicketStatus status) {
        UUID organizationId = currentUser.getOrganizationId();
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setStatus(status);
        MaintenanceTicket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }
}
