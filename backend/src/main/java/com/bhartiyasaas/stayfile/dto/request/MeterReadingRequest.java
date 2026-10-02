package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @Builder.Default
    @Pattern(regexp = "ELECTRICITY|WATER|GAS", message = "Meter type must be ELECTRICITY, WATER, or GAS")
    private String meterType = "ELECTRICITY";

    @DecimalMin(value = "0.00", message = "Previous reading cannot be negative")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.00", message = "Current reading cannot be negative")
    private BigDecimal currentReading;

    @Builder.Default
    @DecimalMin(value = "0.00", message = "Rate per unit cannot be negative")
    private BigDecimal ratePerUnit = BigDecimal.valueOf(10.00);

    private LocalDate readingDate;
    private String notes;
}
