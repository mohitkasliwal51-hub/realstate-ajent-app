package com.bhartiyasaas.stayfile.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingResponse {

    private UUID id;
    private UUID organizationId;
    private UUID unitId;
    private String meterType;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal unitsConsumed;
    private BigDecimal ratePerUnit;
    private BigDecimal totalCharge;
    private LocalDate readingDate;
    private Boolean isBilled;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
