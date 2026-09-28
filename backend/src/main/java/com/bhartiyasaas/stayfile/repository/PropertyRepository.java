package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByOrganizationId(UUID organizationId);
    List<Property> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<Property> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
