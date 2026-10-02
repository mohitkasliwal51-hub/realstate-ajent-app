package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMin(value = "0.00", message = "Total collected cannot be negative")
    private BigDecimal totalCollected;

    @DecimalMin(value = "0.00", message = "Commission cannot be negative")
    private BigDecimal commissionAmount;
    @DecimalMin(value = "0.00", message = "Deductions cannot be negative")
    private BigDecimal deductionsAmount;

    @Builder.Default
    private PaymentMode payoutMode = PaymentMode.NET_BANKING;

    private String utrNumber;
    private LocalDate payoutDate;
    private String notes;
}
