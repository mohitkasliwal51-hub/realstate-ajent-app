package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.EsignTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EsignTransactionRepository extends JpaRepository<EsignTransaction, UUID> {
    List<EsignTransaction> findByOrganizationId(UUID organizationId);
    List<EsignTransaction> findByLeaseIdAndOrganizationId(UUID leaseId, UUID organizationId);
    Optional<EsignTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<EsignTransaction> findByOrganizationIdAndTransactionId(UUID organizationId, String transactionId);
}
