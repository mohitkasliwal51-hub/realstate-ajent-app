package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.entity.enums.ReceiptType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptCreateRequest {

    @NotNull(message = "Lease ID is required")
    private UUID leaseId;

    @NotNull(message = "Receipt type is required")
    private ReceiptType receiptType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String transactionReference;
    private LocalDate paymentDate;
    private String notes;
}
