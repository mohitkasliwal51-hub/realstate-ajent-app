package com.bhartiyasaas.stayfile.repository;

import com.bhartiyasaas.stayfile.entity.BbpsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BbpsTransactionRepository extends JpaRepository<BbpsTransaction, UUID> {
    List<BbpsTransaction> findByOrganizationId(UUID organizationId);
    List<BbpsTransaction> findByPropertyIdAndOrganizationId(UUID propertyId, UUID organizationId);
    Optional<BbpsTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<BbpsTransaction> findByOrganizationIdAndTransactionRef(UUID organizationId, String transactionRef);
}
