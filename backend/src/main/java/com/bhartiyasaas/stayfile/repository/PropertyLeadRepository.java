package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.PropertyLead;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyLeadRepository extends JpaRepository<PropertyLead, UUID> {
    List<PropertyLead> findByOrganizationId(UUID organizationId);
    Optional<PropertyLead> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
