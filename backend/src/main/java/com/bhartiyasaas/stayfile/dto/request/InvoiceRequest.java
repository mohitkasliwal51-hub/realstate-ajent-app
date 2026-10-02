package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.ChargeType;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "Lease ID is required")
    private UUID leaseId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.MONTHLY_RENT;

    @NotNull(message = "Billing period start is required")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end is required")
    private LocalDate billingPeriodEnd;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private String notes;
    private List<LineItemRequest> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItemRequest {
        private ChargeType chargeType;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }
}
