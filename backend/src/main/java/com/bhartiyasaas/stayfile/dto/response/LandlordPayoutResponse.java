package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.entity.enums.PayoutStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordPayoutResponse {

    private UUID id;
    private String payoutNumber;
    private UUID organizationId;
    private UUID landlordId;
    private UUID propertyId;
    private BigDecimal totalCollected;
    private BigDecimal commissionAmount;
    private BigDecimal deductionsAmount;
    private BigDecimal netPayoutAmount;
    private PaymentMode payoutMode;
    private PayoutStatus payoutStatus;
    private String utrNumber;
    private LocalDate payoutDate;
    private String notes;
    private String statementPdfUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
