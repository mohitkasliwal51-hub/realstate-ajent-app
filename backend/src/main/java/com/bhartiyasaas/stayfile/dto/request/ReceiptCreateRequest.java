package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.entity.enums.ReceiptType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
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
public class ReceiptCreateRequest {

    private UUID invoiceId;

    @NotNull(message = "Lease ID is required")
    private UUID leaseId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @Builder.Default
    private ReceiptType receiptType = ReceiptType.RENT_PAYMENT;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.UPI;

    private String transactionReference;

    @Builder.Default
    private LocalDate paymentDate = LocalDate.now();

    private String notes;
}
