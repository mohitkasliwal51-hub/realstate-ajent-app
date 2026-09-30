package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.SharingType;
import com.bhartiyasaas.stayfile.entity.enums.UnitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitResponse {

    private UUID id;
    private UUID propertyId;
    private UUID organizationId;
    private String unitNumber;
    private Integer floorNumber;
    private SharingType sharingType;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private UnitStatus status;
    private UUID currentLeaseId;
    private String amenities;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
