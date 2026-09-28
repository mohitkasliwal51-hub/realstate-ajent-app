package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "esign_transactions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsignTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @Column(nullable = false)
    private String provider;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "stamp_paper_number")
    private String stampPaperNumber;

    @Column(name = "stamp_amount")
    private BigDecimal stampAmount;

    @Column(nullable = false)
    private String status;

    @Column(name = "signed_pdf_url")
    private String signedPdfUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_trail_json", columnDefinition = "jsonb")
    private String auditTrailJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
