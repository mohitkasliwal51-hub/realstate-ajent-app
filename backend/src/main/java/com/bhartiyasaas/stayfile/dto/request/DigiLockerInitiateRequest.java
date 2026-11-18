package com.bhartiyasaas.stayfile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerInitiateRequest {

    private UUID tenantId;

    private String redirectUrl;

    @Builder.Default
    private String userFlow = "signup";
}
