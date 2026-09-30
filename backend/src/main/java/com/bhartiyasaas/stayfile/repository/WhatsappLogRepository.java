package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.WhatsappLog;

import java.util.List;
import java.util.UUID;

@Repository
public interface WhatsappLogRepository extends JpaRepository<WhatsappLog, UUID> {
    List<WhatsappLog> findByOrganizationId(UUID organizationId);
}
