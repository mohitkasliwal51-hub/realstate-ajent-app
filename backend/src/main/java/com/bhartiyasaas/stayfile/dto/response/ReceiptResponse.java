package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.entity.enums.ReceiptType;
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
public class ReceiptResponse {

    private UUID id;
    private String receiptNumber;
    private UUID organizationId;
    private UUID invoiceId;
    private UUID leaseId;
    private UUID tenantId;
    private ReceiptType receiptType;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String transactionReference;
    private LocalDate paymentDate;
    private String notes;
    private String pdfUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
