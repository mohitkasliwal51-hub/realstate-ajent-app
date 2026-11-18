package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.IntegrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, UUID> {
    List<IntegrationLog> findByOrganizationId(UUID organizationId);
    List<IntegrationLog> findByProvider(String provider);
    List<IntegrationLog> findByOrganizationIdAndProvider(UUID organizationId, String provider);
    List<IntegrationLog> findTop50ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
