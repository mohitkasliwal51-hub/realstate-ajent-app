package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.BrandingSettings;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandingSettingsRepository extends JpaRepository<BrandingSettings, UUID> {
    Optional<BrandingSettings> findByOrganizationId(UUID organizationId);
}
