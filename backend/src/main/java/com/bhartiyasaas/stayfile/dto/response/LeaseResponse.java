package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.MaintenanceFeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseResponse {

    private UUID id;
    private UUID organizationId;
    private UUID unitId;
    private UUID tenantId;
    private UUID landlordId;
    private String landlordName;
    private UUID createdById;
    private UUID agreementTemplateId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer rentDueDay;
    private Integer noticePeriodDays;
    private Integer lockInPeriodMonths;

    private BrokerageFeeType brokerageFeeType;
    private BigDecimal brokerageAmount;
    private MaintenanceFeeType maintenanceFeeType;
    private BigDecimal maintenanceFeeAmount;
    private BigDecimal agreementFeeAmount;

    private String customClauses;
    private String termsAndConditions;
    private LeaseStatus status;
    private String agreementPdfUrl;
    private Boolean isEsignCompleted;
    private String esignTransactionId;
    @com.fasterxml.jackson.annotation.JsonProperty("eStampNumber")
    private String eStampNumber;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
