package com.bhartiyasaas.stayfile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.LandlordPayout;
import com.bhartiyasaas.stayfile.entity.enums.PayoutStatus;

@Repository
public interface LandlordPayoutRepository extends JpaRepository<LandlordPayout, UUID> {
    List<LandlordPayout> findByOrganizationId(UUID organizationId);
    List<LandlordPayout> findByLandlordIdAndOrganizationId(UUID landlordId, UUID organizationId);
    List<LandlordPayout> findByOrganizationIdAndPayoutStatus(UUID organizationId, PayoutStatus status);
    Optional<LandlordPayout> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
