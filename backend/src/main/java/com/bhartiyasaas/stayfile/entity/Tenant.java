package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private Profile user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "permanent_address", nullable = false)
    private String permanentAddress;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation")
    private String emergencyContactRelation;

    @Builder.Default
    @Column(name = "id_proof_type")
    private String idProofType = "Aadhaar";

    @Column(name = "id_proof_last4", length = 20)
    private String idProofLast4;

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Column(name = "id_proof_document_url")
    private String idProofDocumentUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kyc_status", nullable = false)
    @Builder.Default
    private VerificationStatus kycStatus = VerificationStatus.PENDING;

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
