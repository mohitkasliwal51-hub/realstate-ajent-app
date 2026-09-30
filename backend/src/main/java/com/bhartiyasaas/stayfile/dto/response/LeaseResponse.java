package com.bhartiyasaas.stayfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseResponse {

    private UUID id;
    private UUID organizationId;
    private UUID unitId;
    private String unitNumber;
    private UUID tenantId;
    private String tenantName;
    private UUID ownerId;
    private String ownerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer rentDueDay;
    private Integer noticePeriodDays;
    private Integer lockInPeriodMonths;
    private String customClauses;
    private String termsAndConditions;
    private LeaseStatus status;
    private String agreementPdfUrl;
    private Boolean isEsignCompleted;
    private String esignTransactionId;
    private String eStampNumber;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
