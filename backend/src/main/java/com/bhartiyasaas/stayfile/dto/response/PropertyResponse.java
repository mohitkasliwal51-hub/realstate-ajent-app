package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.PropertyType;
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
public class PropertyResponse {

    private UUID id;
    private UUID organizationId;
    private UUID landlordId;
    private String landlordName;
    private String name;
    private PropertyType type;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String amenities;
    private String[] rules;
    private String[] images;
    private String description;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
