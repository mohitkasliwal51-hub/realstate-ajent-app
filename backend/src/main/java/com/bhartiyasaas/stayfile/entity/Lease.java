package com.bhartiyasaas.stayfile.entity;

import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "leases", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lease {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Profile owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_template_id")
    private AgreementTemplate agreementTemplate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit", nullable = false)
    private BigDecimal securityDeposit;

    @Builder.Default
    @Column(name = "rent_due_day", nullable = false)
    private Integer rentDueDay = 5;

    @Builder.Default
    @Column(name = "notice_period_days")
    private Integer noticePeriodDays = 30;

    @Builder.Default
    @Column(name = "lock_in_period_months")
    private Integer lockInPeriodMonths = 6;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_clauses", columnDefinition = "jsonb")
    private String customClauses;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private LeaseStatus status = LeaseStatus.ACTIVE;

    @Column(name = "agreement_pdf_url")
    private String agreementPdfUrl;

    @Builder.Default
    @Column(name = "is_esign_completed")
    private Boolean isEsignCompleted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
