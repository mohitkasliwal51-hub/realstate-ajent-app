package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordPayoutRequest {

    @NotNull(message = "Landlord ID is required")
    private UUID landlordId;

    private UUID propertyId;

    @NotNull(message = "Total collected is required")
    private BigDecimal totalCollected;

    private BigDecimal commissionAmount;
    private BigDecimal deductionsAmount;

    @NotNull(message = "Net payout amount is required")
    private BigDecimal netPayoutAmount;

    @Builder.Default
    private PaymentMode payoutMode = PaymentMode.NET_BANKING;

    private String utrNumber;
    private LocalDate payoutDate;
    private String notes;
}
