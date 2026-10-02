package com.bhartiyasaas.stayfile.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceStatus;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByOrganizationId(UUID organizationId);
    List<Invoice> findByLeaseId(UUID leaseId);
    List<Invoice> findByTenantId(UUID tenantId);
    List<Invoice> findByOrganizationIdAndStatus(UUID organizationId, InvoiceStatus status);
        Optional<Invoice> findByLeaseIdAndBillingPeriodStartAndInvoiceType(
            UUID leaseId, LocalDate billingPeriodStart, InvoiceType invoiceType);
    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Invoice> findByOrganizationIdAndInvoiceNumber(UUID organizationId, String invoiceNumber);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = :overdueStatus WHERE i.dueDate < :currentDate AND i.status IN (:unpaidStatus, :partialStatus)")
    int updateOverdueInvoices(@Param("currentDate") LocalDate currentDate, 
                              @Param("overdueStatus") InvoiceStatus overdueStatus, 
                              @Param("unpaidStatus") InvoiceStatus unpaidStatus, 
                              @Param("partialStatus") InvoiceStatus partialStatus);
}

