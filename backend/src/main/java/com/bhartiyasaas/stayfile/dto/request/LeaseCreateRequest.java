package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseCreateRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    private UUID agreementTemplateId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Monthly rent is required")
    private BigDecimal monthlyRent;

    @NotNull(message = "Security deposit is required")
    private BigDecimal securityDeposit;

    @Builder.Default
    private Integer rentDueDay = 5;

    @Builder.Default
    private Integer noticePeriodDays = 30;

    @Builder.Default
    private Integer lockInPeriodMonths = 6;

    private String customClauses;

    private String termsAndConditions;

    @Builder.Default
    private LeaseStatus status = LeaseStatus.ACTIVE;
}
