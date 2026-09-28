package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "branding_settings", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "legal_business_name", nullable = false)
    private String legalBusinessName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "owner_pan")
    private String ownerPan;

    @Column(name = "owner_gstin")
    private String ownerGstin;

    @Column(name = "rera_number")
    private String reraNumber;

    @Column(name = "registered_office_address")
    private String registeredOfficeAddress;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "agency_logo_url")
    private String agencyLogoUrl;

    @Builder.Default
    @Column(name = "primary_color")
    private String primaryColor = "#2563eb";

    @Builder.Default
    @Column(name = "secondary_color")
    private String secondaryColor = "#1e293b";

    @Column(name = "signature_url")
    private String signatureUrl;

    @Column(name = "owner_upi_id")
    private String ownerUpiId;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
