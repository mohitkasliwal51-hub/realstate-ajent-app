package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.AgreementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementTemplateRepository extends JpaRepository<AgreementTemplate, UUID> {
    List<AgreementTemplate> findByOrganizationId(UUID organizationId);
    Optional<AgreementTemplate> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
