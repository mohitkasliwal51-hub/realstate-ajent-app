package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.Landlord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandlordRepository extends JpaRepository<Landlord, UUID> {
    List<Landlord> findByManagingOrganizationId(UUID organizationId);
    List<Landlord> findByManagingOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<Landlord> findByIdAndManagingOrganizationId(UUID id, UUID organizationId);
}
