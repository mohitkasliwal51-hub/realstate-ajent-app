package com.bhartiyasaas.stayfile.dto.request;

import java.time.LocalDate;

import com.bhartiyasaas.stayfile.entity.enums.PayoutStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutStatusUpdateRequest {
    @NotNull(message = "Payout status is required")
    private PayoutStatus payoutStatus;
    private String utrNumber;
    private LocalDate payoutDate;
    private String notes;
}
