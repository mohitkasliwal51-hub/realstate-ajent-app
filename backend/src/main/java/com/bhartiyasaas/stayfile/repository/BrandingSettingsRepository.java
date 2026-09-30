package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.BrandingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrandingSettingsRepository extends JpaRepository<BrandingSettings, UUID> {
	Optional<BrandingSettings> findByOrganizationId(UUID organizationId);
}
