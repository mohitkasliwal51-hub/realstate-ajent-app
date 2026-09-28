package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.bhartiyasaas.stayfile.entity.enums.BbpsStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bbps_transactions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BbpsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id")
    private Lease lease;

    @Column(name = "biller_id", nullable = false)
    private String billerId;

    @Column(name = "biller_name", nullable = false)
    private String billerName;

    @Builder.Default
    @Column(name = "customer_param_name")
    private String customerParamName = "Consumer Number";

    @Column(name = "customer_param_value", nullable = false)
    private String customerParamValue;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private BbpsStatus status = BbpsStatus.BILL_FETCHED;

    @Column(name = "bbps_reference_id")
    private String bbpsReferenceId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "receipt_pdf_url")
    private String receiptPdfUrl;

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
