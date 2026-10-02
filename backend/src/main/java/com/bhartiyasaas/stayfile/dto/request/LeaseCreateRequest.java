package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.MaintenanceFeeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseCreateRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    private UUID landlordId;
    private UUID createdById;

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

    @Builder.Default
    private BrokerageFeeType brokerageFeeType = BrokerageFeeType.NONE;
    private BigDecimal brokerageAmount;

    @Builder.Default
    private MaintenanceFeeType maintenanceFeeType = MaintenanceFeeType.NONE;
    private BigDecimal maintenanceFeeAmount;

    private BigDecimal agreementFeeAmount;

    private String customClauses;
    private String termsAndConditions;

    @Builder.Default
    private LeaseStatus status = LeaseStatus.ACTIVE;
}
