package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private UserRole role;
    private UUID organizationId;
    private String organizationName;
    private String organizationSlug;
    private com.bhartiyasaas.stayfile.entity.enums.OrganizationType organizationType;
    private UUID tenantId;
    private String phone;
}