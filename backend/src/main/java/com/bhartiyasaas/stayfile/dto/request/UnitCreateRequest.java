package com.bhartiyasaas.stayfile.dto.request;

import com.bhartiyasaas.stayfile.entity.enums.SharingType;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitCreateRequest {

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    private UUID parentUnitId;

    @NotBlank(message = "Unit number is required")
    private String unitNumber;

    @Builder.Default
    private Integer floorNumber = 0;

    @Builder.Default
    private SharingType sharingType = SharingType.FULL_FLAT;

    @NotNull(message = "Monthly rent is required")
    private BigDecimal monthlyRent;

    @NotNull(message = "Security deposit is required")
    private BigDecimal securityDeposit;

    @Builder.Default
    private UnitStatus status = UnitStatus.AVAILABLE;

    private String amenities;
    private String notes;
}
