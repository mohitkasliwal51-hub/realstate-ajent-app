package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShowcaseBrandingResponse {
    private String legalBusinessName;
    private String tradeName;
    private String ownerGstin;
    private String contactPhone;
    private String contactEmail;
    private String agencyLogoUrl;
    private String primaryColor;
    private String secondaryColor;
}