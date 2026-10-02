package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "meter_readings", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "meter_type", nullable = false)
    @Builder.Default
    private String meterType = "ELECTRICITY";

    @Column(name = "previous_reading", nullable = false)
    @Builder.Default
    private BigDecimal previousReading = BigDecimal.ZERO;

    @Column(name = "current_reading", nullable = false)
    private BigDecimal currentReading;

    @Column(name = "units_consumed", insertable = false, updatable = false)
    private BigDecimal unitsConsumed;

    @Column(name = "rate_per_unit", nullable = false)
    @Builder.Default
    private BigDecimal ratePerUnit = BigDecimal.valueOf(10.00);

    @Column(name = "total_charge", insertable = false, updatable = false)
    private BigDecimal totalCharge;

    @Column(name = "reading_date", nullable = false)
    @Builder.Default
    private LocalDate readingDate = LocalDate.now();

    @Column(name = "is_billed")
    @Builder.Default
    private Boolean isBilled = false;

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
