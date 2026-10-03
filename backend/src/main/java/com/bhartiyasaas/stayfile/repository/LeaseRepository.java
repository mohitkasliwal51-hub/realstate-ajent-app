package com.bhartiyasaas.stayfile.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, UUID> {
    List<Lease> findByOrganizationId(UUID organizationId);
    List<Lease> findByOrganizationIdAndStatus(UUID organizationId, LeaseStatus status);
    Optional<Lease> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<Lease> findByTenantId(UUID tenantId);
    Optional<Lease> findFirstByTenantIdAndStatus(UUID tenantId, LeaseStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lease l WHERE l.unit.id = :unitId AND l.organization.id = :organizationId AND l.status IN (com.bhartiyasaas.stayfile.entity.enums.LeaseStatus.ACTIVE, com.bhartiyasaas.stayfile.entity.enums.LeaseStatus.PENDING_ESIGN) AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<Lease> findOverlappingLeases(
            @Param("unitId") UUID unitId,
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

        @Query("SELECT l FROM Lease l WHERE l.unit.id = :unitId AND l.organization.id = :organizationId AND l.status IN :statuses " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<Lease> findOverlappingLeases(
            @Param("unitId") UUID unitId,
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaseStatus> statuses
    );
}
