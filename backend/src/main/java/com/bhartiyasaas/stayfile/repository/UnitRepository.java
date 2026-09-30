package com.bhartiyasaas.stayfile.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {
    List<Unit> findByPropertyId(UUID propertyId);
    List<Unit> findByPropertyIdAndOrganizationId(UUID propertyId, UUID organizationId);
    List<Unit> findByPropertyIdAndOrganizationIdAndStatus(UUID propertyId, UUID organizationId, UnitStatus status);
    List<Unit> findByOrganizationIdAndStatus(UUID organizationId, UnitStatus status);
    Optional<Unit> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByPropertyIdAndOrganizationIdAndStatus(UUID propertyId, UUID organizationId, UnitStatus status);
    long countByPropertyIdAndOrganizationId(UUID propertyId, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Unit u WHERE u.id = :id")
    Optional<Unit> findByIdForUpdate(@Param("id") UUID id);
}
