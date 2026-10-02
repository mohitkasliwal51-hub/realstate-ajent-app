package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.ChargeType;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin(value = "0.00", message = "Tax amount cannot be negative")
    private BigDecimal taxAmount;
    @DecimalMin(value = "0.00", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    private String notes;
    private List<@Valid LineItemRequest> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItemRequest {
        private ChargeType chargeType;
        private String description;
        @DecimalMin(value = "0.00", message = "Quantity cannot be negative")
        private BigDecimal quantity;
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        private BigDecimal unitPrice;
        @DecimalMin(value = "0.00", message = "Amount cannot be negative")
        private BigDecimal amount;
    }
}
