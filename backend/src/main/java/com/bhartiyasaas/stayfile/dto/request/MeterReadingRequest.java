package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotNull;
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
    private String meterType = "ELECTRICITY";

    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    private BigDecimal currentReading;

    @Builder.Default
    private BigDecimal ratePerUnit = BigDecimal.valueOf(10.00);

    private LocalDate readingDate;
    private String notes;
}
