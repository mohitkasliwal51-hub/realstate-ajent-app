package com.bhartiyasaas.stayfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Profile owner;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "alternate_phone")
    private String alternatePhone;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "permanent_address", nullable = false)
    private String permanentAddress;

    private String occupation;

    @Column(name = "organization_or_college")
    private String organizationOrCollege;

    @Column(name = "work_address")
    private String workAddress;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation")
    private String emergencyContactRelation;

    @Builder.Default
    @Column(name = "id_proof_type")
    private String idProofType = "Aadhaar";

    @Column(name = "id_proof_last4", length = 4)
    private String idProofLast4;

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Builder.Default
    @Column(name = "is_id_verified")
    private Boolean isIdVerified = false;

    @Column(name = "id_proof_front_url")
    private String idProofFrontUrl;

    @Column(name = "id_proof_back_url")
    private String idProofBackUrl;

    @Column(name = "tenant_photo_url")
    private String tenantPhotoUrl;

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
