package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, UUID> {
    List<Lease> findByOrganizationId(UUID organizationId);
    List<Lease> findByOrganizationIdAndStatus(UUID organizationId, LeaseStatus status);
    Optional<Lease> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Lease> findByUnitIdAndStatus(UUID unitId, LeaseStatus status);
}
