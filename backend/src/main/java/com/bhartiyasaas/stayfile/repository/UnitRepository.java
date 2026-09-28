package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {
    List<Unit> findByPropertyId(UUID propertyId);
    List<Unit> findByOrganizationIdAndStatus(UUID organizationId, UnitStatus status);
    Optional<Unit> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
