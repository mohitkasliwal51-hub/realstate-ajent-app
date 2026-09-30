package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import com.bhartiyasaas.stayfile.entity.enums.PropertyType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyCreateRequest {

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotBlank(message = "Property name is required")
    private String name;

    @NotNull(message = "Property type is required")
    private PropertyType type;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String amenities;
    private String[] rules;
    private String[] images;
    private String description;
}
