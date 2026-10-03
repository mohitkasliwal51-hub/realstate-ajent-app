package com.bhartiyasaas.stayfile.entity.enums;

import lombok.Getter;

/**
 * What an uploaded file is for. The category, never the client, decides whether the
 * file is publicly readable and which content types are acceptable.
 */
@Getter
public enum FileCategory {
    PROPERTY_IMAGE("property-images", true, false),
    ORG_LOGO("org-logos", true, false),
    SIGNATURE("signatures", false, false),
    TENANT_KYC("tenant-kyc", false, true);

    private final String folder;
    private final boolean publicAccess;
    private final boolean pdfAllowed;

    FileCategory(String folder, boolean publicAccess, boolean pdfAllowed) {
        this.folder = folder;
        this.publicAccess = publicAccess;
        this.pdfAllowed = pdfAllowed;
    }
}
