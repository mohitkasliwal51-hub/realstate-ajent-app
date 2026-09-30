package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.MaintenanceTicket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {
    List<MaintenanceTicket> findByOrganizationId(UUID organizationId);
    List<MaintenanceTicket> findByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId);
    Optional<MaintenanceTicket> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
