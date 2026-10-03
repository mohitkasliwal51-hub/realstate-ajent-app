package com.bhartiyasaas.stayfile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Receipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByOrganizationId(UUID organizationId);
    Optional<Receipt> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<Receipt> findByTenantId(UUID tenantId);
    Optional<Receipt> findByOrganizationIdAndReceiptNumber(UUID organizationId, String receiptNumber);
    Optional<Receipt> findTopByOrganizationIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(
            UUID organizationId, String prefix);
}
