package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    List<Tenant> findByOrganizationId(UUID organizationId);
    Optional<Tenant> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Tenant> findByOrganizationIdAndEmail(UUID organizationId, String email);
    Optional<Tenant> findByOrganizationIdAndPhone(UUID organizationId, String phone);
}
